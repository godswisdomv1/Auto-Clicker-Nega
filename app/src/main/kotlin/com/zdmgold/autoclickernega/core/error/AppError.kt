package com.zdmgold.autoclickernega.core.error

/**
 * The error taxonomy. Every failure has a class and a defined treatment. The UI maps each class to
 * a message, so nothing here carries user-facing text.
 */
sealed class AppError(
    val code: String,
    val recoverable: Boolean,
    val retriable: Boolean,
) {
    data object ServiceNotEnabled : AppError("service_not_enabled", recoverable = true, retriable = false)

    data object ServiceKilled : AppError("service_killed", recoverable = true, retriable = false)

    data class StartWaitCancelled(
        val cause: WaitCancelCause,
    ) : AppError("start_wait_cancelled", recoverable = true, retriable = true)

    data class GestureRejected(
        val consecutive: Int,
    ) : AppError("gesture_rejected", recoverable = true, retriable = true)

    data class CaptureUnavailable(
        val cause: CaptureCause,
    ) : AppError("capture_unavailable", recoverable = true, retriable = true)

    data object NothingFound : AppError("nothing_found", recoverable = true, retriable = true)

    data class LanguagePack(
        val cause: PackCause,
    ) : AppError("language_pack", recoverable = true, retriable = true)

    data object RunGate : AppError("run_gate", recoverable = true, retriable = true)

    data class Storage(
        val detail: String,
    ) : AppError("storage", recoverable = true, retriable = true)

    data class MalformedImport(
        val reason: ImportProblem,
        val detail: String = "",
    ) : AppError("malformed_import", recoverable = false, retriable = false)

    data class SchemaTooNew(
        val found: Int,
        val supported: Int,
    ) : AppError("schema_too_new", recoverable = false, retriable = false)

    data class SdkFailure(
        val sdk: String,
    ) : AppError("sdk_failure", recoverable = true, retriable = true)

    data class Unexpected(
        val detail: String,
    ) : AppError("unexpected", recoverable = false, retriable = false)
}

enum class WaitCancelCause { User, ScreenOff, ServiceLost }

enum class CaptureCause { SecureScreen, TooFrequent, Internal }

enum class PackCause { Missing, DownloadFailed, HashMismatch }

/** Why an import was refused. [code] is the stable name shared with the contract fixtures. */
enum class ImportProblem(
    val code: String,
) {
    BadJson("bad_json"),
    BadBundle("bad_bundle"),
    HashMismatch("hash_mismatch"),
    TooLarge("too_large"),
    TooManyItems("too_many_items"),
    InvalidField("invalid_field"),
    Cycle("cycle"),
    TooDeep("too_deep"),
}
