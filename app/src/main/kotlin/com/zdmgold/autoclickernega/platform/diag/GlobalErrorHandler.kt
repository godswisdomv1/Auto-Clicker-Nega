package com.zdmgold.autoclickernega.platform.diag

/** Saves every uncaught crash to the local log and then lets the system finish the job. */
object GlobalErrorHandler {
    fun install(store: CrashLogStore) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            store.record(error, thread.name)
            previous?.uncaughtException(thread, error)
        }
    }
}
