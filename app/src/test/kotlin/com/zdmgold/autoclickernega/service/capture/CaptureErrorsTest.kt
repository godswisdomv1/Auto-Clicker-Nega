package com.zdmgold.autoclickernega.service.capture

import com.zdmgold.autoclickernega.core.error.CaptureCause
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureErrorsTest {
    @Test
    fun codesMapToCauses() {
        assertEquals(CaptureCause.TooFrequent, CaptureErrors.cause(3))
        assertEquals(CaptureCause.SecureScreen, CaptureErrors.cause(6))
        assertEquals(CaptureCause.Internal, CaptureErrors.cause(1))
        assertEquals(CaptureCause.Internal, CaptureErrors.cause(2))
        assertEquals(CaptureCause.Internal, CaptureErrors.cause(99))
    }
}
