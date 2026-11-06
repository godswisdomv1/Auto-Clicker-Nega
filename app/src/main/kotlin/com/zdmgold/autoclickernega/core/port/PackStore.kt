package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.result.Outcome
import java.io.File

/** A language pack that is on the device, with the hash it was verified against. */
data class InstalledPack(
    val lang: String,
    val sha256: String,
    val installedAtMs: Long,
)

/** Where text detection language packs live, and which ones are installed. */
interface PackStore {
    /** The folder a pack's files belong in. The folder may not exist yet. */
    fun directoryFor(lang: String): Outcome<File>

    suspend fun installed(): Outcome<Map<String, InstalledPack>>

    /** Records a pack as installed once its files are in place and verified. */
    suspend fun markInstalled(pack: InstalledPack): Outcome<Unit>

    /** Deletes a pack's files and its record. Used for a failed or corrupted download. */
    suspend fun remove(lang: String): Outcome<Unit>
}
