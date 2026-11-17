package com.zdmgold.autoclickernega.ui

import androidx.annotation.StringRes
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem

/** The one line shown for each error class. The taxonomy carries no user-facing text, so the mapping lives here. */
@StringRes
fun AppError.messageRes(): Int =
    when (this) {
        AppError.ServiceNotEnabled -> R.string.error_service_off
        AppError.ServiceKilled -> R.string.error_service_killed
        is AppError.StartWaitCancelled -> R.string.error_wait_cancelled
        is AppError.GestureRejected -> R.string.error_gesture_rejected
        is AppError.CaptureUnavailable -> R.string.error_capture
        AppError.NothingFound -> R.string.error_nothing_found
        is AppError.LanguagePack -> R.string.error_language_pack
        AppError.RunGate -> R.string.error_run_gate
        is AppError.Storage -> R.string.error_storage
        is AppError.MalformedImport -> reason.messageRes()
        is AppError.SchemaTooNew -> R.string.error_schema_new
        is AppError.SdkFailure -> R.string.error_sdk
        is AppError.Unexpected -> R.string.error_unexpected
    }

@StringRes
private fun ImportProblem.messageRes(): Int =
    when (this) {
        ImportProblem.BadJson -> R.string.error_import_bad_json
        ImportProblem.BadBundle -> R.string.error_import_bad_bundle
        ImportProblem.HashMismatch -> R.string.error_import_hash
        ImportProblem.TooLarge -> R.string.error_import_too_large
        ImportProblem.TooManyItems -> R.string.error_import_too_many
        ImportProblem.InvalidField -> R.string.error_import_invalid
        ImportProblem.Cycle -> R.string.error_import_cycle
        ImportProblem.TooDeep -> R.string.error_import_too_deep
    }
