package com.kairo.browser

import android.content.Context

class BrowserStore(context: Context) {
    private val prefs = context.getSharedPreferences("kairo_browser", Context.MODE_PRIVATE)

    var searchEngineId: String
        get() = prefs.getString("search_engine", "google") ?: "google"
        set(value) = prefs.edit().putString("search_engine", value).apply()

    var themeId: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) = prefs.edit().putString("theme", value).apply()

    var fontId: String
        get() = prefs.getString("font", "system") ?: "system"
        set(value) = prefs.edit().putString("font", value).apply()

    var homePage: String
        get() = prefs.getString("home", "https://www.google.com") ?: "https://www.google.com"
        set(value) = prefs.edit().putString("home", value).apply()

    fun history(): List<String> = prefs.getStringSet("history", emptySet()).orEmpty().toList().sortedDescending()

    fun addHistory(url: String) {
        val values = history().toMutableList()
        values.remove(url)
        values.add(0, url)
        prefs.edit().putStringSet("history", values.take(60).toSet()).apply()
    }

    fun bookmarks(): List<String> = prefs.getStringSet("bookmarks", emptySet()).orEmpty().toList().sorted()

    fun addBookmark(url: String) {
        val values = bookmarks().toMutableSet()
        values.add(url)
        prefs.edit().putStringSet("bookmarks", values).apply()
    }

    fun removeBookmark(url: String) {
        val values = bookmarks().toMutableSet()
        values.remove(url)
        prefs.edit().putStringSet("bookmarks", values).apply()
    }
}
