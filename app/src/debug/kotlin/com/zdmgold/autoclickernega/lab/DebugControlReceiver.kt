package com.zdmgold.autoclickernega.lab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zdmgold.autoclickernega.NegaApp
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.ScriptCodec
import com.zdmgold.autoclickernega.platform.display.DisplayInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Lets adb drive the lab and the engine without a screen, for the emulator harness. It exists in debug builds only.
 * Results go to logcat under the tag NegaLab. Commands are broadcasts, for example:
 * `adb shell am broadcast -n <package>/com.zdmgold.autoclickernega.lab.DebugControlReceiver -a nega.LAB_FRAME`
 */
class DebugControlReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val graph = (context.applicationContext as NegaApp).graph
        val lab = LabHolder.get(graph, "debug")
        when (intent.action) {
            LAB_REFERENCE_BOX -> {
                lab.captureReferenceBox(
                    Rect(
                        intent.getIntExtra("l", 0),
                        intent.getIntExtra("t", 0),
                        intent.getIntExtra("r", 0),
                        intent.getIntExtra("b", 0),
                    ),
                )
            }

            LAB_LEAK -> {
                lab.runLeakProbe()
            }

            LAB_QUICK -> {
                lab.runQuickTest(wait = 0)
            }

            LAB_CROP -> {
                lab.captureReference(wait = 0)
            }

            LAB_FRAME -> {
                lab.captureFrame(wait = 0)
            }

            LAB_MATCH -> {
                lab.runMatch()
            }

            LAB_REPEAT -> {
                lab.runRepeat(wait = 0)
            }

            LAB_RATE -> {
                lab.runRateProbe(wait = 0)
            }

            PANEL -> {
                say("panel: ${graph.overlay.showPanel()}")
            }

            STOP -> {
                say("stop: ${graph.engine.stop()}")
            }

            RUN -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val json = intent.getStringExtra("json").orEmpty()
                    when (val script = ScriptCodec.decode(json)) {
                        is Outcome.Failure -> {
                            say("run: bad script ${script.error}")
                        }

                        is Outcome.Success -> {
                            say(
                                "run: ${graph.engine.start(RunRequest(script.value, DisplayInfo.screenSpec(context)))}",
                            )
                        }
                    }
                }
            }

            else -> {
                say("unknown action ${intent.action}")
            }
        }
    }

    private fun say(text: String) {
        Log.i(DetectionLab.TAG, text)
    }

    private companion object {
        const val LAB_REFERENCE_BOX = "nega.LAB_REFERENCE_BOX"
        const val LAB_LEAK = "nega.LAB_LEAK"
        const val LAB_QUICK = "nega.LAB_QUICK"
        const val LAB_CROP = "nega.LAB_CROP"
        const val LAB_FRAME = "nega.LAB_FRAME"
        const val LAB_MATCH = "nega.LAB_MATCH"
        const val LAB_REPEAT = "nega.LAB_REPEAT"
        const val LAB_RATE = "nega.LAB_RATE"
        const val PANEL = "nega.PANEL"
        const val STOP = "nega.STOP"
        const val RUN = "nega.RUN"
    }
}
