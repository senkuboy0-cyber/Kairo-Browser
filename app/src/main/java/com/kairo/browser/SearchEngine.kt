package com.kairo.browser

data class SearchEngine(
    val id: String,
    val label: String,
    val queryUrl: String
) {
    fun buildUrl(query: String): String = queryUrl.replace("%s", encode(query))

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        val all = listOf(
            SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
            SearchEngine("brave", "Brave", "https://search.brave.com/search?q=%s"),
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
            SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s")
        )

        fun byId(id: String): SearchEngine = all.firstOrNull { it.id == id } ?: all.first()
    }
}
