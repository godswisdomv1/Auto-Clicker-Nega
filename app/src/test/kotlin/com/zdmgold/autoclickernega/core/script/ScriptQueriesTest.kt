package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Point
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptQueriesTest {
    @Test
    fun onlyAScriptWithAColourConditionNeedsColour() {
        val colour = Condition.Color(Point(1, 1), 0)
        assertTrue(TestScripts.smart(TestScripts.event("e", 1, colour, TestScripts.image)).usesColor())
        assertFalse(TestScripts.smart(TestScripts.event("e", 1, TestScripts.image)).usesColor())
        assertFalse(TestScripts.simple(Action.Stop).usesColor())
    }
}
