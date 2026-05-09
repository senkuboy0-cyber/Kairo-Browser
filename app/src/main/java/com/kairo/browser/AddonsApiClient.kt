package com.kairo.browser

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AddonsApiClient {
    fun latestXpiUrl(item: ExtensionItem): String? {
        val connection = URL(item.apiUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")
        return connection.inputStream.bufferedReader().use { reader ->
            val json = JSONObject(reader.readText())
            json.optJSONObject("current_version")
                ?.optJSONArray("files")
                ?.optJSONObject(0)
                ?.optString("url")
                ?.takeIf { it.isNotBlank() }
        }
    }
}
