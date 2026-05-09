package com.kairo.browser

import android.graphics.Color

data class AppTheme(
    val id: String,
    val name: String,
    val background: Int,
    val surface: Int,
    val elevated: Int,
    val text: Int,
    val muted: Int,
    val accent: Int
) {
    companion object {
        val all = listOf(
            AppTheme("system", "System", Color.rgb(7, 16, 19), Color.rgb(16, 27, 31), Color.rgb(25, 39, 44), Color.WHITE, Color.rgb(169, 184, 189), Color.rgb(24, 200, 167)),
            AppTheme("dark", "Dark", Color.rgb(9, 11, 15), Color.rgb(21, 24, 31), Color.rgb(33, 38, 48), Color.WHITE, Color.rgb(184, 189, 199), Color.rgb(42, 127, 255)),
            AppTheme("light", "Light", Color.rgb(245, 247, 250), Color.WHITE, Color.rgb(234, 238, 244), Color.rgb(14, 19, 27), Color.rgb(86, 96, 112), Color.rgb(0, 111, 238)),
            AppTheme("ocean", "Ocean", Color.rgb(4, 31, 45), Color.rgb(8, 51, 68), Color.rgb(13, 74, 92), Color.WHITE, Color.rgb(177, 218, 228), Color.rgb(34, 211, 238)),
            AppTheme("forest", "Forest", Color.rgb(8, 30, 25), Color.rgb(16, 55, 43), Color.rgb(28, 76, 58), Color.WHITE, Color.rgb(184, 211, 199), Color.rgb(91, 214, 141)),
            AppTheme("sunset", "Sunset", Color.rgb(37, 20, 31), Color.rgb(61, 31, 47), Color.rgb(86, 44, 61), Color.WHITE, Color.rgb(232, 190, 199), Color.rgb(255, 145, 77)),
            AppTheme("amoled", "AMOLED", Color.BLACK, Color.rgb(8, 8, 8), Color.rgb(18, 18, 18), Color.WHITE, Color.rgb(160, 160, 160), Color.rgb(24, 200, 167))
        )

        fun byId(id: String): AppTheme = all.firstOrNull { it.id == id } ?: all.first()
    }
}
