# Kairo Browser

Kairo Browser is a Kotlin Android browser built on Mozilla GeckoView.

## Features

- GeckoView rendering engine
- Address/search bar with Google, Brave, Bing, and DuckDuckGo
- Back, forward, reload, home, share, copy link, and open in external browser actions
- Bookmarks and local browsing history
- Theme picker with system, dark, light, ocean, forest, sunset, and amoled styles
- Font picker with system default plus five bundled app font options
- Firefox Add-ons catalog with 10 popular extensions from addons.mozilla.org
- Modern single-activity Android UI with small animations
- Vector app logo and adaptive launcher icon
- GitHub Actions debug APK build for arm64-v8a, armeabi-v7a, x86, and x86_64

## Build Locally

```bash
gradle :app:assembleDebug
```

The project intentionally uses Gradle from the GitHub Actions runner instead of committing a wrapper jar.

## APK Releases

Push a tag like `v0.1.0` or run the workflow manually. The workflow builds one debug APK per ABI and uploads them to a GitHub Release.
