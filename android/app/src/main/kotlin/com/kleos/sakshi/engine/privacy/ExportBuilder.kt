package com.kleos.sakshi.engine.privacy

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ExportDocument

/**
 * F10: what an export is, version and time and whether raw events were asked for. The host's Exporter writes the contents by
 * reading the stores (package names and times only, never a label or any text).
 * [REFACTOR CANDIDATE: the document type is a minimal contract gap (DOC 3 names it, never defines it); if it grows sections,
 *  move the host Exporter's reads here and keep the host to file writing.]
 */
object ExportBuilder {
    const val EXPORT_VERSION = 1
    fun build(includeRaw: Boolean, asOf: EpochMs) = ExportDocument(EXPORT_VERSION, asOf, includeRaw)
}
