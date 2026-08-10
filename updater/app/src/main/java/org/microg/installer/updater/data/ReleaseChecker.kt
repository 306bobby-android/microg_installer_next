package org.microg.installer.updater.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ReleaseChecker {

    private const val GMS_RELEASES_API = "https://api.github.com/repos/microg/GmsCore/releases/latest"
    private const val GITLAB_AURORA_RELEASES_API = "https://gitlab.com/api/v4/projects/AuroraOSS%2FAuroraStore/releases"
    private const val FDROID_AURORA_API = "https://f-droid.org/api/v1/packages/com.aurora.store"

    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val gmsRelease = fetchGmsRelease()
            val (auroraVersion, auroraDownloadUrl) = fetchAuroraRelease()

            ReleaseInfo(
                tagName = gmsRelease?.tagName ?: "0.3.16.252432",
                gmsUrl = gmsRelease?.gmsUrl,
                gmsVersionName = gmsRelease?.gmsVersionName,
                vendingUrl = gmsRelease?.vendingUrl,
                vendingVersionName = gmsRelease?.vendingVersionName,
                auroraUrl = auroraDownloadUrl,
                auroraVersionName = auroraVersion
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchGmsRelease(): ReleaseInfo? {
        return try {
            val url = URL(GMS_RELEASES_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "microGUpdater-App")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) return null

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val assets: JSONArray = json.optJSONArray("assets") ?: JSONArray()

            var gmsUrl: String? = null
            var vendingUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")

                if (name.startsWith("com.google.android.gms") && name.endsWith(".apk") && !name.contains("-hw") && !name.contains("-user")) {
                    gmsUrl = downloadUrl
                } else if (name.startsWith("com.android.vending") && name.endsWith(".apk") && !name.contains("-hw") && !name.contains("-user")) {
                    vendingUrl = downloadUrl
                }
            }

            ReleaseInfo(
                tagName = tagName,
                gmsUrl = gmsUrl,
                gmsVersionName = tagName,
                vendingUrl = vendingUrl,
                vendingVersionName = tagName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchAuroraRelease(): Pair<String?, String?> {
        try {
            val url = URL(GITLAB_AURORA_RELEASES_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "microGUpdater-App")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonString)
                if (array.length() > 0) {
                    val first = array.getJSONObject(0)
                    val tagName = first.optString("tag_name", "").removePrefix("v")
                    val fDroidData = fetchAuroraFdroid()
                    val downloadUrl = fDroidData.second ?: "https://f-droid.org/repo/com.aurora.store_76.apk"
                    return Pair(tagName, downloadUrl)
                }
            }
        } catch (_: Exception) {}

        return fetchAuroraFdroid()
    }

    private fun fetchAuroraFdroid(): Pair<String?, String?> {
        return try {
            val url = URL(FDROID_AURORA_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "microGUpdater-App")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)
                val pkgs = json.optJSONArray("packages")
                if (pkgs != null && pkgs.length() > 0) {
                    val latest = pkgs.getJSONObject(0)
                    val verName = latest.optString("versionName", "")
                    val verCode = latest.optInt("versionCode", 0)
                    val apkUrl = if (verCode > 0) "https://f-droid.org/repo/com.aurora.store_${verCode}.apk" else null
                    return Pair(verName, apkUrl)
                }
            }
            Pair(null, null)
        } catch (_: Exception) {
            Pair(null, null)
        }
    }
}
