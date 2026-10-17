# SeerrCtrl

**Request movies and shows on your Seerr server from your phone.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/SeerrCtrl?label=download)](https://github.com/VoidedKN0X/SeerrCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

SeerrCtrl is an Android app for [Seerr](https://github.com/seerr-team/seerr) (and the older Jellyseerr and Overseerr). Discover, request and manage movies and series on your self-hosted server, with everything the web UI can do.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/SeerrCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="48"></a>

## Features

- **Sign in** with Jellyfin/Emby (including Quick Connect), Plex, or a Seerr account
- **Discover** with the same sliders and filters as the web UI
- **Request** movies and series, in 4K too, with season picking and advanced options
- **Requests list**: swipe right to approve, left to decline
- **Notifications** when requests are approved or available, with Approve/Decline buttons for managers
- **Upcoming calendar** for your requests and watchlist
- **Issues**: report, comment and resolve
- **Full admin**: users, permissions, Radarr/Sonarr, notification agents, jobs and every server setting
- **App lock** with PIN and fingerprint; tablet and foldable layout

## Requirements

- Android 13 or newer
- Seerr, Jellyseerr or Overseerr

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/SeerrCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates with Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium), then tap the button below on your phone. Obtainium adds SeerrCtrl and installs every new release for you.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/SeerrCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="48"></a>

If the button doesn't open Obtainium, add `https://github.com/VoidedKN0X/SeerrCtrl` in Obtainium by hand.

## Getting started

1. Enter your server address, e.g. `http://192.168.1.10:5055` or `https://requests.example.com`.
2. If your server uses a self-signed certificate, turn on **Allow self-signed certificate** and confirm its fingerprint.
3. Sign in with any method your server offers. What you can do follows your Seerr permissions.

## Tips and troubleshooting

- **Late notifications on ColorOS and similar phones:** in the app go to *More → App settings → Notifications → Background activity* and allow it. On ColorOS, also allow **Auto launch** for SeerrCtrl in the phone's settings.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/SeerrCtrl.git
   cd SeerrCtrl
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
3. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

## Support the project

SeerrCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). Starring the repo and [reporting bugs](https://github.com/VoidedKN0X/SeerrCtrl/issues) helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| **SeerrCtrl** (this app) | Request movies and shows on Seerr / Jellyseerr |

## License

SeerrCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

SeerrCtrl is an unofficial app and is not affiliated with the Seerr, Jellyseerr or Overseerr projects. Movie and series data comes from TMDB through your server.
