# SeerrCtrl

**Request movies and shows on your Seerr server from your phone.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/SeerrCtrl?label=download)](https://github.com/VoidedKN0X/SeerrCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

SeerrCtrl is an Android app for [Seerr](https://github.com/seerr-team/seerr) (and the older Jellyseerr and Overseerr). Discover, request and manage movies and series on your self-hosted server, with everything the web UI can do.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/SeerrCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Sign in** with Jellyfin/Emby (including Quick Connect), Plex, or a Seerr account
- **Discover** with the same sliders and filters as the web UI, and search for titles, people, keywords and studios
- **Title pages** with ratings, cast and crew, collections, recommendations and similar titles, and a Play button for your media server
- **Request** movies and series, in 4K too, with season picking and advanced options
- **Requests list**: swipe right to approve, left to decline
- **Notifications** when your requests are approved, declined, failed or available and when issues get replies; managers also get new requests with Approve/Decline buttons, and new issues
- **Upcoming calendar** for your requests and watchlist, plus your **Watchlist** and **Profile** (your requests, quota and stats)
- **Issues**: report, comment and resolve
- **Full admin**: users, permissions, quotas, the blocklist, Radarr/Sonarr, notification agents, jobs, logs and every server setting
- **App lock** with its own PIN (6+ digits) and optional fingerprint; your sign-in stays encrypted with the PIN while the app is locked
- **Tablet and foldable layout**
- **Private by default:** the app is hidden from screenshots and the recent-apps screen (you can turn this off), titles and names are hidden in notifications on the lock screen, Approve/Decline from a notification needs the phone unlocked, and profile pictures from Gravatar and Plex are off unless you turn them on

## Screenshots

Coming soon.

## Requirements

- Android 13 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- Seerr, Jellyseerr or Overseerr
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/SeerrCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds SeerrCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/SeerrCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/SeerrCtrl` in Obtainium by hand.

## Getting started

1. Enter your server address, e.g. `http://192.168.1.10:5055` or `https://requests.example.com`.
2. If your server uses a self-signed certificate, turn on **Allow self-signed certificate** and confirm its fingerprint.
3. Sign in with any method your server offers. What you can do follows your Seerr permissions.

## Tips and troubleshooting

- **Late notifications on ColorOS and similar phones:** in the app go to *More → App settings → Notifications → Background activity* and allow it. On ColorOS, also allow **Auto launch** for SeerrCtrl in the phone's settings.

## Privacy

**Telemetry-free.** SeerrCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It only talks to the servers you add, plus the image and sign-in services Seerr itself relies on, listed below.

### Other servers the app talks to

Apart from your own Seerr server, SeerrCtrl only contacts:

- **TMDB's image server** (`image.tmdb.org`) for posters, backdrops, photos and studio logos, unless your Seerr admin turned on **Cache images locally** (Settings > General in Seerr). Then every image comes through your Seerr server instead, and TMDB never sees your phone. TMDB sees your IP address and which titles' pictures are loaded.
- **plex.tv**, only while you sign in with Plex.
- **Gravatar and Plex** for other people's profile pictures, only if you turn on **More > App settings > Profile pictures from Gravatar and Plex**. It's off by default, and people's initials show instead.
- Trailers and links to sites like IMDb only open when you tap them.

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

SeerrCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X), or tap **Support SeerrCtrl** under **About** in **More > App settings**. Starring the repo and [reporting bugs](https://github.com/VoidedKN0X/SeerrCtrl/issues) helps too; **Report a bug** in the same About section opens the issues page.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| **SeerrCtrl** (this app) | Request movies and shows on Seerr / Jellyseerr |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |

## License

SeerrCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

SeerrCtrl is an unofficial app and is not affiliated with the Seerr, Jellyseerr or Overseerr projects. Movie and series data comes from TMDB through your server.
