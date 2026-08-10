package org.microg.installer.updater.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.microg.installer.updater.worker.UpdateWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            UpdateWorker.scheduleWork(context)
        }
    }
}
