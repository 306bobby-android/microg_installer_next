package org.microg.installer.updater.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object SystemInstaller {

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        packageName: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val destinationFile = File(context.cacheDir, "$packageName.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val url = URL(apkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            val fileLength = connection.contentLength
            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(destinationFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (inputStream.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    onProgress((total * 100 / fileLength).toInt())
                }
                outputStream.write(data, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            installApk(context, destinationFile, packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun installApk(context: Context, apkFile: File, packageName: String): Boolean {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(packageName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        var sessionId = -1
        try {
            sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            session.openWrite(packageName, 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
                session.fsync(out)
            }

            val intent = Intent(context, InstallerResultReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)
            session.close()
            return true
        } catch (e: Exception) {
            if (sessionId != -1) {
                try {
                    packageInstaller.abandonSession(sessionId)
                } catch (_: Exception) {}
            }
            e.printStackTrace()
            return false
        }
    }
}
