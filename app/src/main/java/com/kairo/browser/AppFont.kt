package com.kairo.browser

import android.graphics.Typeface

data class AppFont(val id: String, val name: String, val family: String) {
    fun typeface(): Typeface = Typeface.create(family, Typeface.NORMAL)

    companion object {
        val all = listOf(
            AppFont("system", "System", "sans"),
            AppFont("inter", "Inter", "sans-serif"),
            AppFont("serif", "Source Serif", "serif"),
            AppFont("mono", "JetBrains Mono", "monospace"),
            AppFont("rounded", "Rounded", "sans-serif-medium"),
            AppFont("condensed", "Condensed", "sans-serif-condensed")
        )

        fun byId(id: String): AppFont = all.firstOrNull { it.id == id } ?: all.first()
    }
}
