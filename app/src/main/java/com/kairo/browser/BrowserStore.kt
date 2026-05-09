package com.kairo.browser

import android.content.Context

class BrowserStore(context: Context) {
    private val prefs = context.getSharedPreferences("kairo_browser", Context.MODE_PRIVATE)

    var searchEngineId: String
        get() = readString("search_engine", "google")
        set(value) = writeString("search_engine", value)

    var themeId: String
        get() = readString("theme", "system")
        set(value) = writeString("theme", value)

    var fontId: String
        get() = readString("font", "system")
        set(value) = writeString("font", value)

    var homePage: String
        get() = readString("home", "https://www.google.com")
        set(value) = writeString("home", value)

    fun history(): List<String> {
        val stored = prefs.getStringSet("history", emptySet<String>()) ?: emptySet()
        return stored.toList().sortedDescending()
    }

    fun addHistory(url: String) {
        val values = history().toMutableList()
        values.remove(url)
        values.add(0, url)
        prefs.edit().putStringSet("history", LinkedHashSet(values.take(60))).apply()
    }

    fun bookmarks(): List<String> {
        val stored = prefs.getStringSet("bookmarks", emptySet<String>()) ?: emptySet()
        return stored.toList().sorted()
    }

    fun addBookmark(url: String) {
        val values = LinkedHashSet(bookmarks())
        values.add(url)
        prefs.edit().putStringSet("bookmarks", values).apply()
    }

    fun removeBookmark(url: String) {
        val values = LinkedHashSet(bookmarks())
        values.remove(url)
        prefs.edit().putStringSet("bookmarks", values).apply()
    }

    private fun readString(key: String, fallback: String): String {
        return prefs.getString(key, fallback) ?: fallback
    }

    private fun writeString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
