package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.Point
import java.util.SplittableRandom

/** Seeded jitter, so a run is reproducible in tests while still varying in real use. */
object Jitter {
    private const val SECOND_AXIS_SALT = -7046029254386353131L
    private const val MIX = 31L

    /** A whole number from -[px] to [px]. Zero when [px] is not positive. */
    fun offset(
        seed: Long,
        px: Int,
    ): Int = if (px <= 0) 0 else SplittableRandom(seed).nextInt(-px, px + 1)

    fun point(
        seed: Long,
        px: Int,
    ): Point = Point(offset(seed, px), offset(seed xor SECOND_AXIS_SALT, px))

    /** A signed timing offset from -[ms] to [ms]. */
    fun timing(
        seed: Long,
        ms: Int,
    ): Long = offset(seed xor SECOND_AXIS_SALT.rotateLeft(1), ms).toLong()

    /** Combines a base seed with indices such as the loop and the step. */
    fun mix(
        seed: Long,
        vararg parts: Int,
    ): Long = parts.fold(seed) { acc, part -> acc * MIX + part }
}
