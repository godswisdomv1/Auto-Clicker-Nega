package com.zdmgold.autoclickernega.lab

import com.zdmgold.autoclickernega.AppGraph

/** One lab for the whole process, shared by the screen and the debug control receiver. */
object LabHolder {
    @Volatile
    private var lab: DetectionLab? = null

    @Synchronized
    fun get(
        graph: AppGraph,
        appVersion: String,
    ): DetectionLab = lab ?: DetectionLab(graph.probe, graph.overlay, appVersion).also { lab = it }
}
