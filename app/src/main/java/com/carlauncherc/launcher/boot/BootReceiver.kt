package com.carlauncherc.launcher.boot

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.carlauncherc.launcher.ui.HomeActivity

/**
 * Best-effort nudge for head units whose firmware does not immediately relaunch the selected
 * HOME activity after boot. Android may still suppress background activity starts; in that
 * case the normal HOME-role dispatch remains the authoritative startup path.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val roleManager = context.getSystemService(RoleManager::class.java)
        val isHome = roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
            roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        if (!isHome) return

        val pending = goAsync()
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                context.startActivity(
                    Intent(context, HomeActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }
                )
            }
            pending.finish()
        }, 1_500L)
    }
}
