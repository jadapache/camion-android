package com.pronavicola.camion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pronavicola.camion.service.MqttForegroundService
import com.pronavicola.camion.util.Log

/**
 * Automatically starts the [MqttForegroundService] on device boot or power on.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot completed event received. Launching continuous MqttForegroundService...")
            MqttForegroundService.startService(context)
        }
    }
}
