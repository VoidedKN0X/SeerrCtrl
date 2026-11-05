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
- **Instant notifications with ntfy** (UnifiedPush): Seerr's webhook wakes the app straight away, set up for you or by hand from the app, with background checks as the fallback
- **Open in ArrCtrl** from movie and series pages, when ArrCtrl is installed
- **Upcoming calendar** for your requests and watchlist, plus your **Watchlist** and **Profile** (your requests, quota and stats)
- **Issues**: report, comment and resolve
- **Full admin**: users, permissions, quotas, the blocklist, Radarr/Sonarr, notification agents, jobs, logs and every server setting
- **App lock** with its own PIN and optional fingerprint unlock. Your sign-in is always encrypted with the Android Keystore; with notifications off it's also sealed with your PIN while the app is locked (the background check needs it when notifications are on). With the lock on, notifications only say what kind of news it is, with no titles, names, posters or buttons
- **Tablet and foldable layout**
- **Private by default:** titles and names are hidden in notifications on the lock screen (and in the notification shade too while the app lock is on), Approve/Decline from a notification needs the phone unlocked (and with the app lock on, SeerrCtrl unlocked too), and profile pictures from Gravatar and Plex are off unless you turn them on. Two switches keep the app out of screenshots and the recent-apps view

## Requirements

- Android 13 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- Seerr, Jellyseerr or Overseerr
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. On your phone, open the [Releases page](https://github.com/VoidedKN0X/SeerrCtrl/releases/latest). Scroll down to **Assets** and tap the file that ends in `.apk` (for example `SeerrCtrl-v1.9.4.apk`). The "Source code" zip and tar.gz files are not the app; you don't need them.
2. Open the downloaded file. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds SeerrCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/SeerrCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/SeerrCtrl` in Obtainium by hand.

## Is it safe to install?

- **Open source.** All of SeerrCtrl's code is in this repo, under the GPL-3.0 licence. Anyone can read it.
- **Built by GitHub, not on a personal computer.** Every release on the Releases page is built from this code by [the build workflow](.github/workflows/build.yml) on GitHub Actions.
- **Always signed with the same key.** Android refuses to install an update that's signed with a different key, so nobody else can replace your SeerrCtrl with their own version. The CtrlF-Droid repo only accepts APKs signed with this key too. Its SHA-256 certificate fingerprint is:

  ```
  92:08:DF:BB:6B:CD:BF:5B:AA:9C:EA:6C:5D:C6:D2:51:C2:80:46:56:35:01:51:AC:19:E8:CD:66:D6:98:3D:83
  ```

  Advanced users can compare it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).

**Check a download.** Every release is built and published by GitHub Actions, never on a personal computer:

- The release notes end with the APK's **SHA-256** checksum. Compare it with `sha256sum SeerrCtrl-vX.Y.Z.apk` (or `Get-FileHash` on Windows).
- Each release APK has a signed **build provenance attestation**. With the [GitHub CLI](https://cli.github.com/), `gh attestation verify SeerrCtrl-vX.Y.Z.apk -R VoidedKN0X/SeerrCtrl` proves it was built by this repo's workflow from the tagged commit.
- The signing-certificate fingerprint above stays the same for every release.

**Permissions**

| Permission | What SeerrCtrl uses it for |
| --- | --- |
| Internet | Talking to your Seerr server, and loading posters (see [Privacy](#privacy)) |
| View network connections | Waiting for a network connection before the background check runs |
| Notifications | Showing notifications about requests and issues (Android asks you first) |
| Ignore battery optimizations | Asking Android, only when you tap **Background activity** (Settings › Notifications), to let the background check run on time. SeerrCtrl never asks on its own |
| Use biometric hardware | Optional fingerprint unlock for the app lock |
| Run at startup | Added by Android's job scheduler (WorkManager), so the background check for notifications is set up again after the phone restarts. Only used while notifications are on |
| Prevent phone from sleeping | Added by WorkManager, so a background check that has started can finish. Only while a check runs |
| App-internal broadcasts (`com.vknxworks.seerrctrl.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) | Added by AndroidX so that only SeerrCtrl itself can send to its own internal receivers. It isn't something you grant and gives no access to anything |
| Hide other apps' floating windows over the PIN screen | While sign-in, the lock or PIN screen, a screen with API keys, passwords or tokens, or a delete, log-out or certificate confirmation is showing, other apps' floating windows (chat heads, overlays) are hidden over it, so nothing can cover or imitate it. No prompt; overlays work as usual everywhere else. |

SeerrCtrl doesn't use location, contacts, camera, storage or phone permissions, and has no foreground service.

What SeerrCtrl talks to and what it keeps private is explained under [Privacy](#privacy).

## Getting started

1. Enter your server address, e.g. `http://192.168.1.10:5055` or `https://requests.example.com`.
2. If your server uses a self-signed certificate, turn on **Allow self-signed certificate** and confirm its fingerprint.
3. Sign in with any method your server offers. What you can do follows your Seerr permissions.

Your profile, account settings, the server, **Settings**, log out and **About** are behind your profile picture at the top right of every tab. The **More** tab has your watchlist, the upcoming calendar and, if you manage the server, users, the blocklist and server settings.

## Tips and troubleshooting

- **Late notifications on ColorOS and similar phones:** in the app go to *tap your profile picture › Settings › Notifications › Background activity* and allow it. On ColorOS, also allow **Auto launch** for SeerrCtrl in the phone's settings.

## App lock

Turn it on: tap your profile picture › **Settings › Privacy and security › App lock**. It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, such as the fingerprint sensor, checked by the phone's secure hardware. Face unlock that only uses the camera isn't accepted. The PIN is still needed after the phone restarts and at least every 72 hours, and adding a new fingerprint to the phone turns fingerprint unlock off until you turn it on again.
- **Your sign-in is encrypted with the PIN while SeerrCtrl is locked.** The key only exists in memory while the app is unlocked and is wiped the moment it locks. The PIN itself is never stored: it's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware, so a copy of the app's files isn't enough to guess it on a computer.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Opening a link, a trailer, the calendar or Android's settings from SeerrCtrl don't count as leaving, unless you stay away for more than 5 minutes.
- **Change app PIN** asks for your current PIN first, and turning the app lock off asks for the PIN too.
- **Forgot your PIN?** It can't be recovered, because it encrypts your sign-in. The lock screen offers to log out and delete what SeerrCtrl keeps on this phone, so you can sign in again; your Seerr server and requests aren't touched.
- **With the app lock on**, the recent-apps screen always shows a blank card.
- **Notifications keep working while locked**: while notifications are on, the background check needs your session when the app isn't open, so the session is then encrypted only with the Android Keystore, as without the app lock. Turn notifications off to keep it sealed with your PIN too; the App lock setting says which applies.

**Hide in screenshots** and **Hide in recent apps** are in the same section, work without the app lock too, and are both off by default. The first keeps SeerrCtrl out of screenshots and screen recordings; the second shows a blank card in the recent-apps view instead of the app. Sign-in, the lock screen, the PIN screens, the server settings (with their API keys and passwords), user settings and the push setup screen are always kept out of screenshots, whatever the switches say.

## Privacy

**Telemetry-free.** SeerrCtrl has no analytics, crash reporting, usage stats, update checks or tracking of any kind. It only talks to the servers you add, plus the image and sign-in services Seerr itself relies on, listed below. Your session is only ever sent to your own Seerr server, never across a redirect to another site, and plain http is only used for your server after you confirm the "Not encrypted" warning.

### Other servers the app talks to

Apart from your own Seerr server, SeerrCtrl only contacts:

- **TMDB's image server** (`image.tmdb.org`) for posters, backdrops, photos and studio logos, unless your Seerr admin turned on **Cache images locally** (Settings > General in Seerr). Then every image comes through your Seerr server instead, and TMDB never sees your phone. TMDB sees your IP address and which titles' pictures are loaded.
- **plex.tv**, only while you sign in with Plex.
- **Gravatar and Plex** for other people's profile pictures, only if you turn on **Profile pictures from Gravatar and Plex** (tap your profile picture › **Settings › Privacy and security**). It's off by default, and people's initials show instead.
- **Your ntfy server**, only if you turn on **Instant notifications**. The ntfy app keeps the connection; SeerrCtrl itself only posts to its push address when you tap **Send a test push**. Pushes carry no titles or names.
- **Plex's sign-in page** (`app.plex.tv`) opens in your browser while you sign in with Plex.
- Trailers and links to sites like IMDb, Seerr's documentation and GitHub page (server settings), F-Droid's ntfy page, ArrCtrl and the Support, Report a bug and Licence links only open when you tap them.

Nothing is backed up: SeerrCtrl is left out of Android's cloud backup and its phone-to-phone transfer.

## How SeerrCtrl is made

SeerrCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

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

**Dependency verification.** Every library and build plugin download is checked against the sha256 checksums in `gradle/verification-metadata.xml`, and the build fails on any mismatch. When you change a dependency, a plugin or the Gradle version, run the **Dependency verification metadata** workflow (Actions tab, *Run workflow*), check the diff of the file it prints and commit it in the same push.

## Support the project

SeerrCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X), or tap **Support SeerrCtrl** under **About** on the **You** page (tap your profile picture). Starring the repo and [reporting bugs](https://github.com/VoidedKN0X/SeerrCtrl/issues/new/choose) helps too; **Report a bug** in the same About section opens the issues page. Bug reports and ideas use a short form that asks for what's needed.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CallCtrl](https://github.com/VoidedKN0X/CallCtrl) | Phone app: dialer, call history and in-call screen |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [ClockCtrl](https://github.com/VoidedKN0X/ClockCtrl) | Alarms, timers, stopwatch and world clock |
| [ContactCtrl](https://github.com/VoidedKN0X/ContactCtrl) | Contacts: browse, edit, import and export |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [DateCtrl](https://github.com/VoidedKN0X/DateCtrl) | Calendar for the calendars on your phone and your accounts |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [LijnCtrl](https://github.com/VoidedKN0X/LijnCtrl) | Live De Lijn bus and tram departures |
| [MailCtrl](https://github.com/VoidedKN0X/MailCtrl) | Email for Gmail and Proton Mail |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [RecorderCtrl](https://github.com/VoidedKN0X/RecorderCtrl) | Private, offline voice recorder |
| [RideCtrl](https://github.com/VoidedKN0X/RideCtrl) | Offline maintenance, fuel and mod log for your vehicles |
| **SeerrCtrl** (this app) | Request movies and shows on Seerr / Jellyseerr |
| [SumCtrl](https://github.com/VoidedKN0X/SumCtrl) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TextCtrl](https://github.com/VoidedKN0X/TextCtrl) | Private SMS and MMS messaging |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

SeerrCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

Fonts: [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 (licence texts in `app/src/main/assets/licenses/`). They're bundled in the app, so nothing is downloaded at runtime.

SeerrCtrl is an independent app and isn't made by or affiliated with the Seerr, Jellyseerr or Overseerr projects. Movie and series data comes from TMDB through your server.
