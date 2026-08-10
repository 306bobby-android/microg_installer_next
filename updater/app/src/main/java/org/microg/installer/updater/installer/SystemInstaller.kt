package org.microg.installer.updater.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

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

            var currentUrl = apkUrl
            var connection: HttpURLConnection
            var responseCode: Int
            var redirects = 0

            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                connection.connect()

                responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {
                    val loc = connection.getHeaderField("Location")
                    if (loc != null && redirects < 5) {
                        currentUrl = loc
                        redirects++
                        continue
                    }
                }
                break
            }

            if (responseCode != 200) {
                Log.e("microGInstaller", "Download failed with HTTP code $responseCode")
                return@withContext false
            }

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

            val installed = installApk(context, destinationFile, packageName)
            if (!installed) {
                fallbackInstallIntent(context, destinationFile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun installApk(context: Context, apkFile: File, packageName: String): Boolean {
        // 1. Try non-blocking silent shell pm install
        if (installSilentlyViaPm(apkFile)) {
            Log.d("microGInstaller", "Silent pm install succeeded for $packageName")
            return true
        }

        // 2. Try privileged PackageInstaller session API
        return installViaPackageInstaller(context, apkFile, packageName)
    }

    private fun installSilentlyViaPm(apkFile: File): Boolean {
        val commands = arrayOf(
            arrayOf("su", "-c", "pm install -r -d --user 0 \"${apkFile.absolutePath}\""),
            arrayOf("su", "0", "pm", "install", "-r", "-d", apkFile.absolutePath)
        )

        for (cmd in commands) {
            try {
                val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
                val completed = process.waitFor(5, TimeUnit.SECONDS)
                if (completed) {
                    val output = process.inputStream.bufferedReader().readText()
                    if (process.exitValue() == 0 || output.contains("Success", ignoreCase = true)) {
                        return true
                    }
                } else {
                    process.destroyForcibly()
                }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun installViaPackageInstaller(context: Context, apkFile: File, packageName: String): Boolean {
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

            val intent = Intent(context, InstallerResultReceiver::class.java).apply {
                putExtra("target_package_name", packageName)
            }
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

    private fun fallbackInstallIntent(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
