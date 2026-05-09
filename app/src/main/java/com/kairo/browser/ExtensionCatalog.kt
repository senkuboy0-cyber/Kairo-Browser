package com.kairo.browser

data class ExtensionItem(
    val slug: String,
    val name: String,
    val summary: String,
    val apiUrl: String = "https://addons.mozilla.org/api/v5/addons/addon/$slug/"
)

object ExtensionCatalog {
    val popular = listOf(
        ExtensionItem("ublock-origin", "uBlock Origin", "Efficient content blocker"),
        ExtensionItem("privacy-badger17", "Privacy Badger", "Tracker protection"),
        ExtensionItem("darkreader", "Dark Reader", "Dark mode for websites"),
        ExtensionItem("bitwarden-password-manager", "Bitwarden", "Password manager"),
        ExtensionItem("ghostery", "Ghostery", "Privacy and ad blocking"),
        ExtensionItem("decentraleyes", "Decentraleyes", "Local CDN resource protection"),
        ExtensionItem("clearurls", "ClearURLs", "Removes tracking parameters"),
        ExtensionItem("sponsorblock", "SponsorBlock", "Skip sponsored video segments"),
        ExtensionItem("https-everywhere", "HTTPS Everywhere", "HTTPS upgrade rules"),
        ExtensionItem("tampermonkey", "Tampermonkey", "Userscript manager")
    )
}
