package com.zdmgold.autoclickernega.ui.home

import com.zdmgold.autoclickernega.core.error.WaitCancelCause
import com.zdmgold.autoclickernega.core.model.EngineBlockReason
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.port.ScriptMode
import com.zdmgold.autoclickernega.core.port.ScriptSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelTest {
    private fun row(
        name: String,
        updated: Long,
    ) = ScriptSummary(name.lowercase(), name, ScriptMode.Simple, 1, updated)

    private val library = listOf(row("Beta", 20), row("alpha", 10), row("Gamma Reward", 30))

    @Test
    fun searchAppearsOnlyAboveEightScripts() {
        assertFalse(HomeModel.showSearch(8))
        assertTrue(HomeModel.showSearch(9))
        assertFalse(HomeUiState(totalCount = 3).showSearch)
        assertTrue(HomeUiState(totalCount = 12).showSearch)
    }

    @Test
    fun recentPutsTheNewestFirst() {
        assertEquals(
            listOf("Gamma Reward", "Beta", "alpha"),
            HomeModel.visible(library, "", HomeSort.Recent).map { it.name },
        )
    }

    @Test
    fun nameIgnoresCase() {
        assertEquals(
            listOf("alpha", "Beta", "Gamma Reward"),
            HomeModel.visible(library, "", HomeSort.Name).map { it.name },
        )
    }

    @Test
    fun theQueryFiltersByAnyPartOfTheNameIgnoringCaseAndSpaces() {
        assertEquals(listOf("Gamma Reward"), HomeModel.visible(library, "  reward ", HomeSort.Recent).map { it.name })
        assertTrue(HomeModel.visible(library, "zzz", HomeSort.Recent).isEmpty())
    }

    @Test
    fun equalNamesOrTimesStayInAStableOrder() {
        val twins = listOf(row("Same", 5), row("same", 9))
        assertEquals(listOf(9L, 5L), HomeModel.visible(twins, "", HomeSort.Name).map { it.updatedAtMs })
    }

    @Test
    fun aRunIsActiveWhileCountingWaitingRunningOrPaused() {
        assertTrue(HomeModel.isActive(EngineState.Waiting("s", 1000, 5000)))
        assertTrue(HomeModel.isActive(EngineState.Countdown("s", 3)))
        assertTrue(HomeModel.isActive(EngineState.Running("s", 0, null)))
        assertTrue(HomeModel.isActive(EngineState.Paused("s")))
        assertFalse(HomeModel.isActive(EngineState.Idle))
        assertFalse(HomeModel.isActive(EngineState.ServiceLost))
        assertFalse(HomeModel.isActive(EngineState.Blocked(EngineBlockReason.GesturesRejected)))
        assertTrue(HomeUiState(engine = EngineState.Running("s", 0, null)).runActive)
        assertEquals(WaitCancelCause.User, WaitCancelCause.valueOf("User"))
    }
}
