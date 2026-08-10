package org.microg.installer.updater.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast

class InstallerResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME) ?: "Package"

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d("microGInstaller", "User action required for $packageName")
                val confirmationIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmationIntent != null) {
                    confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmationIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("microGInstaller", "Successfully installed $packageName")
                Toast.makeText(context, "Successfully installed $packageName", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e("microGInstaller", "Failed to install $packageName: $message (status: $status)")
                Toast.makeText(context, "Install failed for $packageName: ${message ?: status}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
