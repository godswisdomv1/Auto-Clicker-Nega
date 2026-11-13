package com.zdmgold.autoclickernega.platform.diag

import com.zdmgold.autoclickernega.core.error.AppError

/** The classes of the most recent errors, newest last. Only the stable error code is kept, never any detail. */
class ErrorLog(
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val codes = ArrayDeque<String>()

    @Synchronized
    fun record(error: AppError) {
        if (codes.size == capacity) codes.removeFirst()
        codes.addLast(error.code)
    }

    /** How many times each error class was seen among the kept errors. */
    @Synchronized
    fun counts(): Map<String, Int> = codes.groupingBy { it }.eachCount()

    @Synchronized
    fun clear() = codes.clear()

    private companion object {
        const val DEFAULT_CAPACITY = 50
    }
}
