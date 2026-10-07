package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.ports.Randomness
import com.kleos.sakshi.engine.tuning.Tuning
import kotlin.math.sqrt

/**
 * Optional, hand-written k-means++ over window vectors (no ML library, per
 * DOC4's agent prompt hint). Gated by week >= CLUSTER_MIN_WEEK (approximated
 * here as ctx.weeks.size, since PatternContext carries no separate "current
 * week number") and >= CLUSTER_MIN_WINDOWS qualifying windows.
 */
object Clustering : PatternDetector {
    override val kind = PatternKind.SHAPE

    override fun detect(ctx: PatternContext): List<Pattern> {
        if (ctx.weeks.size < Tuning.CLUSTER_MIN_WEEK) return emptyList()
        val windows = qualifyingWindows(ctx)
        if (windows.size < Tuning.CLUSTER_MIN_WINDOWS) return emptyList()

        val rawVectors = windows.map { vectorOf(it) }
        val zVectors = zScore(rawVectors)
        val overallStaysPerHour = median(windows.map { staysPerHourOf(it) })

        var bestK = -1
        var bestAssignment: List<Int>? = null
        var bestSilhouette = -1.0
        for (k in 2..Tuning.CLUSTER_MAX_K) {
            val random = ctx.random.fork(Tuning.CLUSTER_SEED + k)
            val assignment = kMeans(zVectors, k, random)
            val silhouette = meanSilhouette(zVectors, assignment, k)
            if (silhouette > bestSilhouette) {
                bestSilhouette = silhouette
                bestAssignment = assignment
                bestK = k
            }
        }
        if (bestAssignment == null || bestSilhouette < Tuning.CLUSTER_MIN_SILHOUETTE) return emptyList()

        val patterns = mutableListOf<Pattern>()
        for (c in 0 until bestK) {
            val memberIndices = bestAssignment.indices.filter { bestAssignment[it] == c }
            if (memberIndices.size < Tuning.CLUSTER_MIN_SIZE) continue
            val members = memberIndices.map { windows[it] }

            val cells = members.map { Rhythm.cellKey(it, ctx.zone) }
            val topCell = cells.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: continue
            val cellShare = topCell.value.toDouble() / members.size
            if (cellShare < Tuning.CLUSTER_CELL_SHARE) continue

            val centreStaysPerHour = members.map { staysPerHourOf(it) }.average()
            if (centreStaysPerHour < 1.5 * overallStaysPerHour) continue

            val stoneShare = stoneShareOf(members)
            val name = if (stoneShare >= 0.60) "Pinged" else "Reached"

            patterns += Pattern(
                kind = PatternKind.SHAPE,
                key = "cluster:${topCell.key}",
                strength = cellShare,
                evidenceWindows = members.size,
                evidenceDays = members.map { it.window.day }.distinct().size,
                firstSeen = ctx.asOf,
                lastSeen = ctx.asOf,
                args = mapOf("cell" to topCell.key, "name" to name, "size" to members.size.toString()),
            )
        }
        return patterns
    }

    // [stretchMin, staysPerHour, firstStayMin, stoneShare, returnMin, quietShare]
    private fun vectorOf(wd: WindowWithDetail): DoubleArray {
        val stretchMin = wd.stretches.sumOf { it.minutes }
        val staysPerHour = staysPerHourOf(wd)
        val firstStayMin = wd.stays.minByOrNull { it.start.value }
            ?.let { (it.start.value - wd.window.start.value) / 60_000.0 } ?: 0.0
        val stoneShare = stoneShareOf(listOf(wd))
        val returns = wd.stays.mapNotNull { it.returnMinutes }
        val returnMin = if (returns.isEmpty()) 0.0 else median(returns)
        val quietShare = wd.quietMinutes / ((wd.window.end.value - wd.window.start.value) / 60_000.0)
        return doubleArrayOf(stretchMin, staysPerHour, firstStayMin, stoneShare, returnMin, quietShare)
    }

    private fun stoneShareOf(windows: List<WindowWithDetail>): Double {
        val known = windows.flatMap { it.stays }.filter { it.origin == Origin.STONE || it.origin == Origin.SELF_STARTED }
        if (known.isEmpty()) return 0.0
        return known.count { it.origin == Origin.STONE }.toDouble() / known.size
    }

    private fun zScore(vectors: List<DoubleArray>): List<DoubleArray> {
        val columns = vectors[0].indices
        val means = columns.map { c -> vectors.map { it[c] }.average() }
        val stds = columns.map { c ->
            val m = means[c]
            sqrt(vectors.map { (it[c] - m) * (it[c] - m) }.average())
        }
        return vectors.map { v -> DoubleArray(v.size) { c -> if (stds[c] > 0.0) (v[c] - means[c]) / stds[c] else 0.0 } }
    }

    private fun kMeans(points: List<DoubleArray>, k: Int, random: Randomness, maxIterations: Int = 50): List<Int> {
        val centroids = initPlusPlus(points, k, random)
        var assignments = points.map { nearest(it, centroids) }
        repeat(maxIterations) {
            for (c in centroids.indices) {
                val members = points.filterIndexed { i, _ -> assignments[i] == c }
                if (members.isNotEmpty()) centroids[c] = columnMeans(members)
            }
            val next = points.map { nearest(it, centroids) }
            if (next == assignments) return next
            assignments = next
        }
        return assignments
    }

    private fun initPlusPlus(points: List<DoubleArray>, k: Int, random: Randomness): MutableList<DoubleArray> {
        val centroids = mutableListOf(points[random.nextInt(points.size)].copyOf())
        while (centroids.size < k) {
            val distances = points.map { p -> centroids.minOf { c -> squaredDistance(p, c) } }
            val total = distances.sum()
            if (total <= 0.0) {
                centroids += points[random.nextInt(points.size)].copyOf()
                continue
            }
            var r = random.nextDouble() * total
            var chosen = points.size - 1
            for (i in points.indices) {
                r -= distances[i]
                if (r <= 0.0) {
                    chosen = i
                    break
                }
            }
            centroids += points[chosen].copyOf()
        }
        return centroids
    }

    private fun nearest(point: DoubleArray, centroids: List<DoubleArray>): Int =
        centroids.indices.minByOrNull { squaredDistance(point, centroids[it]) } ?: 0

    private fun columnMeans(points: List<DoubleArray>): DoubleArray =
        DoubleArray(points[0].size) { c -> points.map { it[c] }.average() }

    private fun squaredDistance(a: DoubleArray, b: DoubleArray): Double =
        a.indices.sumOf { (a[it] - b[it]) * (a[it] - b[it]) }

    private fun euclidean(a: DoubleArray, b: DoubleArray): Double = sqrt(squaredDistance(a, b))

    private fun meanSilhouette(points: List<DoubleArray>, assignments: List<Int>, k: Int): Double {
        if (k < 2) return 0.0
        val byCluster = (0 until k).map { c -> points.indices.filter { assignments[it] == c } }
        val scores = points.indices.map { i ->
            val own = byCluster[assignments[i]]
            if (own.size <= 1) return@map 0.0
            val a = own.filter { it != i }.map { euclidean(points[i], points[it]) }.average()
            val b = (0 until k).filter { it != assignments[i] }
                .mapNotNull { c -> byCluster[c].takeIf { it.isNotEmpty() }?.map { euclidean(points[i], points[it]) }?.average() }
                .minOrNull() ?: 0.0
            if (a == 0.0 && b == 0.0) 0.0 else (b - a) / maxOf(a, b)
        }
        return scores.average()
    }
}
