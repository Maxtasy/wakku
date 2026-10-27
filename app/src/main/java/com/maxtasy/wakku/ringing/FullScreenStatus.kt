package com.maxtasy.wakku.ringing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Remembers whether the last attempt to open the ringing screen directly
 * failed (RingingService launched it but it never appeared). Some OEMs, MIUI
 * for one, block that behind permissions of their own that no API can query,
 * so the alarm list shows its "allow full screen" banner based on this.
 */
object FullScreenStatus {
    private const val PREFS = "full_screen_status"
    private const val KEY_BLOCKED = "blocked"

    fun isBlocked(context: Context): Boolean = prefs(context).getBoolean(KEY_BLOCKED, false)

    fun setBlocked(context: Context, blocked: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCKED, blocked).apply()
    }

    /** True when the ringing screen can't be relied on to open by itself: no overlay permission, or the last try failed. */
    fun needsAttention(context: Context): Boolean = !Settings.canDrawOverlays(context) || isBlocked(context)

    /**
     * Opens the settings page where the user can fix it: MIUI's own permission
     * editor on Xiaomi devices (it holds the pop-up and lock-screen
     * permissions no API exposes), else the overlay permission page, else the
     * app's general settings. Tries each until one exists on this device.
     */
    fun openSettings(context: Context) {
        val pkg = context.packageName
        val intents = mutableListOf<Intent>()
        if (Build.MANUFACTURER.lowercase() in XIAOMI_BRANDS) {
            intents += Intent("miui.intent.action.APP_PERM_EDITOR")
                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                .putExtra("extra_pkgname", pkg)
            intents += Intent("miui.intent.action.APP_PERM_EDITOR")
                .setPackage("com.miui.securitycenter")
                .putExtra("extra_pkgname", pkg)
        }
        if (!Settings.canDrawOverlays(context)) {
            intents += Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))
        }
        intents += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }

    private val XIAOMI_BRANDS = setOf("xiaomi", "redmi", "poco")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
