package com.zdmgold.autoclickernega.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per script. The script itself is one JSON column, and the rest are indexed for the Home list. */
@Entity(
    tableName = "scripts",
    indices = [Index(value = ["updatedAt"]), Index(value = ["name"])],
)
data class ScriptEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** "simple" or "smart". */
    val mode: String,
    @ColumnInfo(defaultValue = "0") val itemCount: Int,
    val updatedAt: Long,
    val json: String,
)

/** The columns Home needs, so listing scripts never loads their JSON. */
data class ScriptSummaryRow(
    val id: String,
    val name: String,
    val mode: String,
    val itemCount: Int,
    val updatedAt: Long,
)
