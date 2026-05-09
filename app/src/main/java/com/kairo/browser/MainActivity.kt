package com.kairo.browser

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var store: BrowserStore
    private lateinit var runtime: GeckoRuntime
    private lateinit var session: GeckoSession
    private lateinit var geckoView: GeckoView
    private lateinit var root: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var addressBar: EditText
    private lateinit var progress: ProgressBar
    private lateinit var contentFrame: FrameLayout
    private lateinit var bottomBar: LinearLayout
    private var panelView: View? = null
    private var currentUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = BrowserStore(this)
        runtime = GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build())
        session = GeckoSession()
        session.open(runtime)
        buildUi()
        geckoView.setSession(session)
        applyAppearance()

        val incoming = intent?.dataString
        if (incoming == null) {
            showHomeScreen()
        } else {
            load(incoming)
        }
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (panelView != null) {
            closePanel()
        } else {
            super.onBackPressed()
        }
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8))
        }

        val logo = ImageView(this).apply {
            setImageResource(resources.getIdentifier("kairo_logo", "drawable", packageName))
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            setOnClickListener { showHomeScreen() }
        }
        toolbar.addView(logo)

        addressBar = EditText(this).apply {
            singleLine = true
            hint = "Search or enter address"
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(14), dp(8), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(8) }
            setOnEditorActionListener { _, actionId, event ->
                val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    loadFromInput(text.toString())
                    true
                } else false
            }
        }
        toolbar.addView(addressBar)
        toolbar.addView(iconButton("Go") { loadFromInput(addressBar.text.toString()) })
        toolbar.addView(iconButton("Menu") { showMainMenu() })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(-1, dp(3))
        }

        contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        geckoView = GeckoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        contentFrame.addView(geckoView)

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6))
        }
        listOf(
            "Back" to { session.goBack() },
            "Forward" to { session.goForward() },
            "Reload" to { session.reload() },
            "Home" to { showHomeScreen() },
            "Saved" to { showBookmarksScreen() },
            "Settings" to { showSettingsScreen() }
        ).forEach { (label, action) -> bottomBar.addView(navButton(label, action)) }

        root.addView(toolbar)
        root.addView(progress)
        root.addView(contentFrame)
        root.addView(bottomBar)
        setContentView(root)
    }

    private fun showMainMenu() {
        val actions = arrayOf(
            "Home screen",
            "Settings screen",
            "Bookmarks screen",
            "History screen",
            "Extensions screen",
            "Save bookmark",
            "Share page",
            "Copy link",
            "Open externally",
            "Set current as home"
        )
        AlertDialog.Builder(this)
            .setTitle("Kairo Browser")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showHomeScreen()
                    1 -> showSettingsScreen()
                    2 -> showBookmarksScreen()
                    3 -> showHistoryScreen()
                    4 -> showExtensionsScreen()
                    5 -> bookmarkCurrent()
                    6 -> shareCurrent()
                    7 -> copyCurrent()
                    8 -> openExternally()
                    9 -> { store.homePage = currentUrl.ifBlank { store.homePage }; toast("Home page updated") }
                }
            }
            .show()
    }

    private fun showHomeScreen() {
        val theme = AppTheme.byId(store.themeId)
        val panel = screen("Home")
        panel.addView(TextView(this).apply {
            text = "Kairo Browser"
            textSize = 26f
            setTextColor(theme.text)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        })
        val search = EditText(this).apply {
            hint = "Search with ${SearchEngine.byId(store.searchEngineId).label}"
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(theme.text)
            setHintTextColor(theme.muted)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = rounded(theme.elevated, dp(24))
            setOnEditorActionListener { _, actionId, event ->
                val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    loadFromInput(text.toString())
                    true
                } else false
            }
        }
        panel.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18) })
        val shortcuts = listOf(
            "Google" to "https://www.google.com",
            "YouTube" to "https://www.youtube.com",
            "GitHub" to "https://github.com",
            "News" to "https://news.google.com"
        )
        shortcuts.forEach { (label, url) -> panel.addView(rowButton(label, url) { load(url) }) }
        panel.addView(sectionTitle("Recent"))
        store.history().take(6).forEach { url -> panel.addView(rowButton(shortUrl(url), url) { load(url) }) }
        showPanel(panel)
    }

    private fun showSettingsScreen() {
        val theme = AppTheme.byId(store.themeId)
        val panel = screen("Settings")
        panel.addView(sectionTitle("Search engine"))
        SearchEngine.all.forEach { engine ->
            panel.addView(rowButton(engine.label, if (engine.id == store.searchEngineId) "Selected" else "Tap to select") {
                store.searchEngineId = engine.id
                showSettingsScreen()
            })
        }
        panel.addView(sectionTitle("Theme"))
        AppTheme.all.forEach { appTheme ->
            panel.addView(rowButton(appTheme.name, if (appTheme.id == store.themeId) "Selected" else "Apply") {
                store.themeId = appTheme.id
                applyAppearance()
                showSettingsScreen()
            })
        }
        panel.addView(sectionTitle("Font"))
        AppFont.all.forEach { font ->
            panel.addView(rowButton(font.name, if (font.id == store.fontId) "Selected" else "Apply") {
                store.fontId = font.id
                applyAppearance()
                showSettingsScreen()
            })
        }
        panel.addView(sectionTitle("Browser tools"))
        panel.addView(rowButton("Set current page as home", currentUrl.ifBlank { store.homePage }) {
            store.homePage = currentUrl.ifBlank { store.homePage }
            toast("Home page updated")
        })
        panel.addView(rowButton("Copy current link", currentUrl.ifBlank { "No page loaded" }) { copyCurrent() })
        panel.setBackgroundColor(theme.background)
        showPanel(panel)
    }

    private fun showBookmarksScreen() {
        val panel = screen("Bookmarks")
        panel.addView(rowButton("Save current page", currentUrl.ifBlank { "No page loaded" }) { bookmarkCurrent(); showBookmarksScreen() })
        val items = store.bookmarks()
        if (items.isEmpty()) panel.addView(emptyText("No bookmarks saved yet"))
        items.forEach { url -> panel.addView(rowButton(shortUrl(url), url) { load(url) }) }
        showPanel(panel)
    }

    private fun showHistoryScreen() {
        val panel = screen("History")
        val items = store.history()
        if (items.isEmpty()) panel.addView(emptyText("No browsing history yet"))
        items.forEach { url -> panel.addView(rowButton(shortUrl(url), url) { load(url) }) }
        showPanel(panel)
    }

    private fun showExtensionsScreen() {
        val panel = screen("Extensions")
        panel.addView(emptyText("Popular Firefox Add-ons. Install opens the Firefox Add-ons API package when GeckoView allows it, otherwise the add-on page opens."))
        ExtensionCatalog.popular.forEach { item ->
            panel.addView(rowButton(item.name, item.summary) { installExtension(item) })
        }
        showPanel(panel)
    }

    private fun screen(title: String): LinearLayout {
        val theme = AppTheme.byId(store.themeId)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18))
            setBackgroundColor(theme.background)
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 22f
                setTextColor(theme.text)
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun showPanel(panel: LinearLayout) {
        closePanel()
        val scroll = ScrollView(this).apply {
            setBackgroundColor(AppTheme.byId(store.themeId).background)
            addView(panel)
            alpha = 0f
            translationY = dp(24).toFloat()
        }
        panelView = scroll
        contentFrame.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        scroll.animate().alpha(1f).translationY(0f).setDuration(180).setInterpolator(DecelerateInterpolator()).start()
        applyAppearance()
    }

    private fun closePanel() {
        panelView?.let { contentFrame.removeView(it) }
        panelView = null
    }

    private fun sectionTitle(textValue: String): TextView {
        val theme = AppTheme.byId(store.themeId)
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            setTextColor(theme.muted)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(20), 0, dp(8))
        }
    }

    private fun rowButton(title: String, subtitle: String, action: () -> Unit): LinearLayout {
        val theme = AppTheme.byId(store.themeId)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14))
            background = rounded(theme.elevated, dp(14))
            setOnClickListener { action() }
            addView(TextView(this@MainActivity).apply { text = title; textSize = 16f; setTextColor(theme.text) })
            addView(TextView(this@MainActivity).apply { text = subtitle; textSize = 12f; setTextColor(theme.muted) })
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        }
    }

    private fun emptyText(value: String): TextView {
        val theme = AppTheme.byId(store.themeId)
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(theme.muted)
            setPadding(0, dp(12), 0, dp(12))
        }
    }

    private fun installExtension(item: ExtensionItem) {
        toast("Fetching ${item.name}")
        thread {
            val result = runCatching { AddonsApiClient().latestXpiUrl(item) }.getOrNull()
            runOnUiThread {
                if (result == null) {
                    openUrl("https://addons.mozilla.org/firefox/addon/${item.slug}/")
                    toast("Opened add-on page")
                } else {
                    runCatching {
                        runtime.webExtensionController.install(result)
                        toast("Install requested: ${item.name}")
                    }.onFailure {
                        openUrl("https://addons.mozilla.org/firefox/addon/${item.slug}/")
                        toast("Opened add-on page")
                    }
                }
            }
        }
    }

    private fun loadFromInput(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        val url = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> SearchEngine.byId(store.searchEngineId).buildUrl(value)
        }
        load(url)
    }

    private fun load(url: String) {
        closePanel()
        currentUrl = url
        addressBar.setText(url)
        progress.progress = 35
        animateBars()
        store.addHistory(url)
        session.loadUri(url)
        progress.postDelayed({ progress.progress = 100 }, 450)
        progress.postDelayed({ progress.progress = 0 }, 900)
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun bookmarkCurrent() {
        if (currentUrl.isBlank()) {
            toast("No page loaded")
            return
        }
        store.addBookmark(currentUrl)
        toast("Bookmark saved")
    }

    private fun shareCurrent() {
        if (currentUrl.isBlank()) return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentUrl)
        }, "Share page"))
    }

    private fun copyCurrent() {
        if (currentUrl.isBlank()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Kairo URL", currentUrl))
        toast("Link copied")
    }

    private fun openExternally() {
        if (currentUrl.isNotBlank()) openUrl(currentUrl)
    }

    private fun applyAppearance() {
        val theme = AppTheme.byId(store.themeId)
        val font = AppFont.byId(store.fontId).typeface()
        window.statusBarColor = theme.background
        window.navigationBarColor = theme.background
        root.setBackgroundColor(theme.background)
        toolbar.setBackgroundColor(theme.surface)
        bottomBar.setBackgroundColor(theme.surface)
        addressBar.setTextColor(theme.text)
        addressBar.setHintTextColor(theme.muted)
        addressBar.background = rounded(theme.elevated, dp(24))
        applyFont(root, font)
        tintNavText(root, theme)
    }

    private fun applyFont(view: View, typeface: Typeface) {
        if (view is TextView) view.typeface = typeface
        if (view is ViewGroup) for (i in 0 until view.childCount) applyFont(view.getChildAt(i), typeface)
    }

    private fun tintNavText(view: View, theme: AppTheme) {
        if (view is TextView && view.parent == toolbar || view is TextView && view.parent == bottomBar) {
            view.setTextColor(theme.text)
            view.background = rounded(theme.elevated, dp(18))
        }
        if (view is Button) {
            view.setTextColor(theme.text)
            view.background = rounded(theme.elevated, dp(18))
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) tintNavText(view.getChildAt(i), theme)
    }

    private fun iconButton(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 12f
        setPadding(dp(10), 0, dp(10), 0)
        layoutParams = LinearLayout.LayoutParams(dp(64), dp(44)).apply { marginStart = dp(6) }
        setOnClickListener { action() }
    }

    private fun navButton(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 12f
        setPadding(dp(4))
        layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(3); marginEnd = dp(3) }
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun animateBars() {
        toolbar.translationY = -toolbar.height.toFloat().coerceAtLeast(20f)
        toolbar.animate().translationY(0f).setDuration(180).setInterpolator(DecelerateInterpolator()).start()
        bottomBar.alpha = 0.55f
        bottomBar.animate().alpha(1f).setDuration(220).start()
    }

    private fun shortUrl(url: String): String = url.removePrefix("https://").removePrefix("http://").take(48)

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
