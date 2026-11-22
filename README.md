# AetherTV

A modern, lightweight Android IPTV player built with **Kotlin** and **Jetpack Compose**, designed with a **Subtle Matte White Liquid Glass** interface.

## Features

- **Xtream Codes & M3U Support** — Connect with Xtream Codes credentials or any M3U/M3U8 playlist URL.
- **Live TV, Movies & Series** — Browse by category, **All**, or **Favorites**, with streaming JSON parsing for large playlists.
- **Built-in ExoPlayer (Media3)** — Smooth HLS/TS/MP4 playback with fullscreen rotation, aspect ratio modes (Fit / Fill / Zoom), Picture-in-Picture (PiP), and real-time stream stats (resolution, FPS, codecs, bitrate).
- **External Player Handoff** — Open any stream directly in your preferred external video player.
- **Editorial Detail View** — Full-bleed poster view with plot, rating, release year, and inline season/episode picker for series.
- **Home Dashboard** — Quick access to Movies & Series Favorites and Recently Watched history.

## Installation

1. Go to the [Releases](../../releases) page.
2. Download the latest `AetherTV-v2.apk`.
3. Install it on your Android device (Android 8.0+ / API 26+).

## Tech Stack

- **UI:** Jetpack Compose (Material 3)
- **Player:** AndroidX Media3 ExoPlayer
- **Networking & Images:** OkHttp, Coil
- **CI/CD:** GitHub Actions

## Disclaimer

AetherTV does **not** provide, host, or include any IPTV channels, playlists, or media content. Users must supply their own playlists or Xtream Codes credentials.

## License

MIT License — see [LICENSE](LICENSE) for details.
