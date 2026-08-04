# Browser - Production-Ready Lightweight Android Web Browser

[![Build Android Application](https://github.com/naveen/browser/actions/workflows/build.yml/badge.svg)](https://github.com/naveen/browser/actions)

A complete, high-performance, modern Android Web Browser written in 100% Java using Material Design components. Built for Android 6.0 (API 23) and above.

## Features

- **Fast & Lightweight**: Minimal memory usage, optimized Android WebView integration.
- **Modern UI**: Dark toolbar, top address bar with search engine integration, bottom navigation bar.
- **Search Engines**: Multi-search support (Google, DuckDuckGo, Bing, Brave, Yahoo).
- **Tab Management**: Multi-tab support with real-time switching, adding, and closing tabs.
- **Incognito Mode**: Private browsing mode without recording history or storing cookies.
- **Night Mode**: Force dark mode or dark reader stylesheet for web pages.
- **Media & Uploads**: Supports full-screen HTML5 videos, camera captures, image picks, and file uploads.
- **Download Integration**: Full system DownloadManager integration with progress and notifications.
- **Bookmark & History**: Persistent storage, real-time search, bookmarking, and history clearing.
- **Find in Page**: Search text on active web pages with forward/backward navigation.
- **Reader Mode**: Simple text extraction for clutter-free reading.
- **Intent Handling**: Direct handling of `tel:`, `mailto:`, `geo:`, `intent:`, and `youtube:` links.
- **Security & Privacy**: Zero analytics, zero ad trackers, SSL warning handlings, cookie & cache controls.

## Screenshots

*(Place screenshot images here)*

## Build Instructions

1. Clone this repository:
   ```bash
   git clone https://github.com/naveen/browser.git
   ```
2. Open the project in Android Studio (Jellyfish or newer recommended).
3. Sync Gradle project files.
4. Run the app on an emulator or Android device running Android 6.0+ (API 23+):
   ```bash
   ./gradlew assembleDebug
   ```

## License

MIT License. Free for commercial and non-commercial use.
