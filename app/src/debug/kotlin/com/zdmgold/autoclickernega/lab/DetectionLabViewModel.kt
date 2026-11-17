package com.zdmgold.autoclickernega.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zdmgold.autoclickernega.AppGraph
import kotlinx.coroutines.flow.StateFlow

/** The screen's handle on the lab. The lab itself lives in [LabHolder], so a run goes on if the screen is left. */
class DetectionLabViewModel(
    private val lab: DetectionLab,
) : ViewModel() {
    val state: StateFlow<LabState> get() = lab.state

    fun captureReference() = lab.captureReference()

    fun captureFrame() = lab.captureFrame()

    fun runMatch() = lab.runMatch()

    fun runRepeat() = lab.runRepeat()

    fun runRateProbe() = lab.runRateProbe()

    fun cancel() = lab.cancel()

    fun report(): String = lab.report()

    companion object {
        fun factory(
            graph: AppGraph,
            appVersion: String,
        ) = viewModelFactory { initializer { DetectionLabViewModel(LabHolder.get(graph, appVersion)) } }
    }
}
