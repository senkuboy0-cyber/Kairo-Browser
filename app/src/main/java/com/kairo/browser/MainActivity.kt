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
    private lateinit var bottomBar: LinearLayout
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
        load(incoming ?: store.homePage)
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
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
        }
        toolbar.addView(logo)

        addressBar = EditText(this).apply {
            singleLine = true
            hint = "Search or enter address"
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(Color.TRANSPARENT, dp(24))
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

        geckoView = GeckoView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6))
        }
        listOf(
            "Back" to { session.goBack() },
            "Forward" to { session.goForward() },
            "Reload" to { session.reload() },
            "Home" to { load(store.homePage) },
            "Star" to { bookmarkCurrent() },
            "Share" to { shareCurrent() }
        ).forEach { (label, action) -> bottomBar.addView(navButton(label, action)) }

        root.addView(toolbar)
        root.addView(progress)
        root.addView(geckoView)
        root.addView(bottomBar)
        setContentView(root)
    }

    private fun showMainMenu() {
        val actions = arrayOf(
            "New tab",
            "Search engine",
            "Bookmarks",
            "History",
            "Extensions",
            "Themes",
            "Fonts",
            "Copy link",
            "Open externally",
            "Set current as home"
        )
        AlertDialog.Builder(this)
            .setTitle("Kairo Browser")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> load(store.homePage)
                    1 -> chooseSearchEngine()
                    2 -> showUrlList("Bookmarks", store.bookmarks())
                    3 -> showUrlList("History", store.history())
                    4 -> showExtensions()
                    5 -> chooseTheme()
                    6 -> chooseFont()
                    7 -> copyCurrent()
                    8 -> openExternally()
                    9 -> { store.homePage = currentUrl; toast("Home page updated") }
                }
            }
            .show()
    }

    private fun chooseSearchEngine() {
        val engines = SearchEngine.all
        AlertDialog.Builder(this)
            .setTitle("Search engine")
            .setItems(engines.map { it.label }.toTypedArray()) { _, index ->
                store.searchEngineId = engines[index].id
                toast("Search uses ${engines[index].label}")
            }
            .show()
    }

    private fun chooseTheme() {
        val themes = AppTheme.all
        AlertDialog.Builder(this)
            .setTitle("Theme")
            .setItems(themes.map { it.name }.toTypedArray()) { _, index ->
                store.themeId = themes[index].id
                applyAppearance()
            }
            .show()
    }

    private fun chooseFont() {
        val fonts = AppFont.all
        AlertDialog.Builder(this)
            .setTitle("Font")
            .setItems(fonts.map { it.name }.toTypedArray()) { _, index ->
                store.fontId = fonts[index].id
                applyAppearance()
                toast("Font applied")
            }
            .show()
    }

    private fun showExtensions() {
        val theme = AppTheme.byId(store.themeId)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12))
        }
        ExtensionCatalog.popular.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10))
                background = rounded(theme.elevated, dp(12))
            }
            row.addView(TextView(this).apply { text = item.name; textSize = 16f; setTextColor(theme.text) })
            row.addView(TextView(this).apply { text = item.summary; textSize = 13f; setTextColor(theme.muted) })
            row.addView(Button(this).apply {
                text = "Install from Firefox Add-ons"
                setOnClickListener { installExtension(item) }
            })
            container.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        AlertDialog.Builder(this)
            .setTitle("Extensions")
            .setView(ScrollView(this).apply { addView(container) })
            .setNegativeButton("Close", null)
            .show()
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

    private fun showUrlList(title: String, urls: List<String>) {
        if (urls.isEmpty()) {
            toast("No $title yet")
            return
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(urls.toTypedArray()) { _, index -> load(urls[index]) }
            .show()
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
        if (currentUrl.isBlank()) return
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
        addressBar.typeface = font
        applyFont(root, font)
        tintButtons(root, theme)
    }

    private fun applyFont(view: View, typeface: Typeface) {
        if (view is TextView) view.typeface = typeface
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyFont(view.getChildAt(i), typeface)
        }
    }

    private fun tintButtons(view: View, theme: AppTheme) {
        if (view is Button) {
            view.setTextColor(theme.text)
            view.background = rounded(theme.elevated, dp(18))
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) tintButtons(view.getChildAt(i), theme)
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

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
