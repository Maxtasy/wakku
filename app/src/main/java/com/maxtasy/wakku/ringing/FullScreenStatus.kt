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
    fun isOverlayMissing(context: Context): Boolean = !Settings.canDrawOverlays(context)

    fun needsAttention(context: Context): Boolean = !Settings.canDrawOverlays(context) || isBlocked(context)

    /**
     * Opens the settings page where the user can fix it: first the "display
     * over other apps" page while that permission is missing, since it is the
     * one thing the app can check; then, once it is granted but the screen still
     * failed to open, MIUI's own permission editor on Xiaomi devices (it holds
     * the pop-up and lock-screen permissions no API exposes); finally the app's
     * general settings. Tries each until one exists on this device.
     */
    fun openSettings(context: Context) {
        val pkg = context.packageName
        // Optimistic: the user is about to change the permissions. If the screen
        // still fails to open on the next ring, the flag (and banner) come back.
        setBlocked(context, false)
        val intents = mutableListOf<Intent>()
        if (!Settings.canDrawOverlays(context)) {
            intents += Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))
        }
        if (isXiaomi) {
            intents += Intent("miui.intent.action.APP_PERM_EDITOR")
                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                .putExtra("extra_pkgname", pkg)
            intents += Intent("miui.intent.action.APP_PERM_EDITOR")
                .setPackage("com.miui.securitycenter")
                .putExtra("extra_pkgname", pkg)
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

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
