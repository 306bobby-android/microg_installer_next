package org.microg.installer.updater.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ReleaseChecker {

    private const val RELEASES_API_URL = "https://api.github.com/repos/microg/GmsCore/releases/latest"

    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASES_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "microGUpdater-App")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) {
                return@withContext null
            }

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
}
