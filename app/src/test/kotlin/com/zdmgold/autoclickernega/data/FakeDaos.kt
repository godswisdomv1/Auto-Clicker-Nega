package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.port.WallClock
import com.zdmgold.autoclickernega.data.db.ScriptDao
import com.zdmgold.autoclickernega.data.db.ScriptEntity
import com.zdmgold.autoclickernega.data.db.ScriptSummaryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScriptDao : ScriptDao {
    val rows = MutableStateFlow<Map<String, ScriptEntity>>(emptyMap())

    override fun summaries(): Flow<List<ScriptSummaryRow>> =
        rows.map { all ->
            all.values
                .sortedByDescending { it.updatedAt }
                .map { ScriptSummaryRow(it.id, it.name, it.mode, it.itemCount, it.updatedAt) }
        }

    override suspend fun get(id: String): ScriptEntity? = rows.value[id]

    override suspend fun upsert(entity: ScriptEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String): Int {
        val had = id in rows.value
        rows.value = rows.value - id
        return if (had) 1 else 0
    }
}

class FakeWallClock(
    var now: Long = 1_000L,
) : WallClock {
    override fun epochMs(): Long = now

    override fun localDate(): String = "2026-11-04"
}
