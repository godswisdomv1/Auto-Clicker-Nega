package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterStoreTest {
    private fun op(
        id: String,
        kind: CounterOpKind,
        v: Int,
    ) = Action.CounterOp(id, kind, v)

    @Test
    fun eachCounterStartsAtItsDeclaredValue() {
        val s = CounterStore(listOf(CounterDef("a", "A", 5), CounterDef("b", "B")))
        assertEquals(5, s.get("a"))
        assertEquals(0, s.get("b"))
        assertEquals(0, s.get("unknown"))
    }

    @Test
    fun setAddAndSubtract() {
        val s = CounterStore(listOf(CounterDef("a", "A", 5)))
        s.apply(op("a", CounterOpKind.Add, 3))
        assertEquals(8, s.get("a"))
        s.apply(op("a", CounterOpKind.Subtract, 10))
        assertEquals(-2, s.get("a"))
        s.apply(op("a", CounterOpKind.Set, 42))
        assertEquals(42, s.get("a"))
    }

    @Test
    fun resultsSaturateInsteadOfWrapping() {
        val s = CounterStore(listOf(CounterDef("a", "A", Int.MAX_VALUE - 1)))
        s.apply(op("a", CounterOpKind.Add, 100))
        assertEquals(Int.MAX_VALUE, s.get("a"))
        s.apply(op("a", CounterOpKind.Set, Int.MIN_VALUE + 1))
        s.apply(op("a", CounterOpKind.Subtract, 100))
        assertEquals(Int.MIN_VALUE, s.get("a"))
    }

    @Test
    fun anUnknownCounterIsIgnored() {
        val s = CounterStore(listOf(CounterDef("a", "A", 1)))
        s.apply(op("zzz", CounterOpKind.Add, 1))
        assertEquals(mapOf("a" to 1), s.snapshot())
    }

    @Test
    fun registeringAddsOnlyMissingCountersAndReportsChanges() {
        val seen = mutableListOf<Map<String, Int>>()
        val s = CounterStore(listOf(CounterDef("a", "A", 1))) { seen += it }
        s.register(listOf(CounterDef("a", "A", 99), CounterDef("b", "B", 2)))
        assertEquals(mapOf("a" to 1, "b" to 2), s.snapshot())
        s.apply(op("b", CounterOpKind.Add, 1))
        assertEquals(mapOf("a" to 1, "b" to 3), seen.last())
        val before = seen.size
        s.register(listOf(CounterDef("a", "A", 5)))
        assertEquals(before, seen.size)
    }
}
