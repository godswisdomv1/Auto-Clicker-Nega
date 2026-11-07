package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.port.FrameSource
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame

/** Frames from a function of the call number, so a test can script what the screen shows over time. */
class FakeFrameSource(
    private val provider: (Int) -> Outcome<Frame>,
) : FrameSource {
    var calls = 0
        private set

    override suspend fun next(): Outcome<Frame> = provider(calls++)

    companion object {
        /** The same picture every time, as a fresh copy, because the pipeline hides the panel in place. */
        fun constant(frame: Frame) =
            FakeFrameSource { Outcome.Success(Frame(frame.w, frame.h, frame.px.copyOf(), frame.rgb?.copyOf())) }

        fun of(provider: (Int) -> Frame) = FakeFrameSource { Outcome.Success(provider(it)) }
    }
}
