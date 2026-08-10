package org.microg.installer.updater.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import org.microg.installer.updater.MainActivity
import org.microg.installer.updater.R
import org.microg.installer.updater.data.ReleaseChecker
import org.microg.installer.updater.installer.SystemInstaller
import java.util.concurrent.TimeUnit

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("updater_settings", Context.MODE_PRIVATE)
        val autoCheck = prefs.getBoolean("auto_check_updates", true)

        if (!autoCheck) return Result.success()

        val release = ReleaseChecker.fetchLatestRelease() ?: return Result.retry()

        val gmsVersion = getInstalledVersion("com.google.android.gms")
        val vendingVersion = getInstalledVersion("com.android.vending")

        val gmsNeedsUpdate = release.gmsUrl != null && isNewerVersion(gmsVersion, release.gmsVersionName)
        val vendingNeedsUpdate = release.vendingUrl != null && isNewerVersion(vendingVersion, release.vendingVersionName)

        if (gmsNeedsUpdate || vendingNeedsUpdate) {
            val autoInstall = prefs.getBoolean("auto_install_updates", false)
            if (autoInstall) {
                if (gmsNeedsUpdate && release.gmsUrl != null) {
                    SystemInstaller.downloadAndInstall(context, release.gmsUrl, "com.google.android.gms") {}
                }
                if (vendingNeedsUpdate && release.vendingUrl != null) {
                    SystemInstaller.downloadAndInstall(context, release.vendingUrl, "com.android.vending") {}
                }
            } else {
                showNotification(context, release.tagName)
            }
        }

        return Result.success()
    }

    private fun getInstalledVersion(packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun isNewerVersion(installed: String?, latest: String?): Boolean {
        if (installed == null) return true
        if (latest == null) return false
        return installed != latest
    }

    private fun showNotification(context: Context, versionTag: String) {
        val channelId = context.getString(R.string.notif_channel_id)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText("microG release v$versionTag is available.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }

    companion object {
        fun scheduleWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "microGUpdateCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
