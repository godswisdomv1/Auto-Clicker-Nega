package com.zdmgold.autoclickernega.platform.diag

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Reads the device facts for the diagnostic report. */
object DeviceInfoProvider {
    fun read(context: Context): DeviceInfo {
        val version =
            try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    .orEmpty()
            } catch (_: PackageManager.NameNotFoundException) {
                ""
            }
        return DeviceInfo(version, Build.MODEL.orEmpty(), Build.VERSION.RELEASE.orEmpty(), Build.VERSION.SDK_INT)
    }
}
