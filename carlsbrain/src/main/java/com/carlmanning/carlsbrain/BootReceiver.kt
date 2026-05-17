package com.carlmanning.carlsbrain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule WorkManager jobs for morning digest after device reboot
            // WorkManager tasks survive reboots automatically when using REQUIRE_NETWORK
            // but explicit re-schedule can be added here if needed
        }
    }
}
