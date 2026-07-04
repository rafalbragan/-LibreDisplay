package com.libredisplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.libredisplay.MainActivity

/**
 * BroadcastReceiver that starts the application automatically after device reboot.
 *
 * Requires the RECEIVE_BOOT_COMPLETED permission declared in AndroidManifest.xml.
 *
 * This is an optional feature – if the caregiver does not want auto-start,
 * they can disable this receiver via device settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launchIntent)
        }
    }
}

