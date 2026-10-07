package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.AppInfo
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.ports.AppCatalog

class FakeAppCatalog(
    private var apps: List<AppInfo> = emptyList(),
    private val categories: MutableMap<Pkg, Int> = mutableMapOf(),
    private val neutral: MutableSet<Pkg> = mutableSetOf(),
    private var own: Pkg = Pkg("com.kleos.sakshi"),
) : AppCatalog {
    override fun launcherApps(): List<AppInfo> = apps

    override fun category(pkg: Pkg): Int? = categories[pkg]

    override fun isNeutral(pkg: Pkg): Boolean = pkg in neutral

    override fun ownPackage(): Pkg = own

    fun setLauncherApps(value: List<AppInfo>) {
        apps = value
    }

    fun setCategory(pkg: Pkg, category: Int?) {
        if (category == null) categories.remove(pkg) else categories[pkg] = category
    }

    fun setNeutral(pkg: Pkg, isNeutral: Boolean) {
        if (isNeutral) neutral += pkg else neutral -= pkg
    }

    fun setOwnPackage(pkg: Pkg) {
        own = pkg
    }
}
