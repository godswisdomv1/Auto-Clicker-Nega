package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class StreamReaderTest {
    @Test
    fun readsEverythingUpToTheLimit() {
        val data = ByteArray(20_000) { it.toByte() }
        val r = readCapped(ByteArrayInputStream(data), 20_000) as Outcome.Success
        assertTrue(data.contentEquals(r.value))
        assertTrue((readCapped(ByteArrayInputStream(ByteArray(0)), 10) as Outcome.Success).value.isEmpty())
    }

    @Test
    fun stopsWithATypedErrorPastTheLimit() {
        val r = readCapped(ByteArrayInputStream(ByteArray(20_001)), 20_000) as Outcome.Failure
        assertEquals(ImportProblem.TooLarge, (r.error as AppError.MalformedImport).reason)
    }
}
