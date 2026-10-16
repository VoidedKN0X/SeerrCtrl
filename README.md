# SeerrCtrl

An unofficial Android app for [Seerr](https://github.com/seerr-team/seerr) (and the older Jellyseerr and Overseerr), so you and the people you share with can discover, request and manage movies and series on your self-hosted server from a phone. Built with Kotlin and Jetpack Compose on Seerr's own `/api/v1`.

## Features

**Sign in**
- First start asks for your server address, then offers whichever sign-in methods the server has turned on
- Jellyfin and Emby accounts, including Jellyfin **Quick Connect** (no password typed on the phone)
- **Plex** sign-in through plex.tv in a browser tab
- Seerr's own email-and-password accounts, with "Forgot password?" when email is set up
- Works over plain HTTP on your LAN, through a reverse proxy, or with a self-signed certificate

**Discover**
- The same sliders as the web home page, in the order your admin set: recently added, recent requests, watchlist, trending, popular, upcoming, genres, studios, networks, and custom keyword/genre/studio/network/search/streaming-service sliders
- "See all" opens an endless grid with the web UI's filters: sort, genres, release years, original language, runtime, TMDB rating, vote count and streaming services
- Search for movies, series and people, or by keyword and studio

**Movies and series**
- Backdrop, poster, availability (and 4K availability), runtime or seasons, genres, overview and tagline
- **Request** and **Request 4K**, with season picking for series, request quotas, and the advanced options: server, quality profile, root folder, language profile, tags and "request as" another user
- **Play** on your media server when it's available, trailer, watchlist, and **report an issue** (by season and episode)
- Live download progress from Radarr/Sonarr, ratings from TMDB, Rotten Tomatoes and IMDb, where to stream in your region
- Seasons with every episode, cast and crew, collections (request the whole collection at once), keywords, recommendations and similar titles
- Managers get approve/decline/edit on the title's requests, and a **Manage** sheet: open in Radarr/Sonarr, mark available or partially available, remove from Radarr/Sonarr, clear data, blocklist

**Requests**
- Everyone's requests for managers (pending first, with a badge on the tab), your own otherwise
- Filter by status (pending, approved, processing, available, unavailable, failed, deleted, completed), movies or series, newest or recently changed
- Approve, decline, retry failed, edit, cancel or delete; swipe a pending request right to approve or left to decline

**Issues**
- Open/resolved list with a badge for open issues
- Comment thread, edit or delete comments, resolve, reopen, delete

**People and profiles**
- Person pages with biography and every appearance or crew credit
- Profiles with request count, quotas and the user's requests; watchlists

**Your account**
- Display name, email, Discord ID, discover and streaming region, discover language
- Password
- Notification agents and which events reach you on each (email, Discord, Pushbullet, Pushover, Telegram, web push)

**Notifications**
- Your requests approved, declined, failed or now available
- New requests waiting for approval, with **Approve** and **Decline** buttons in the notification
- New issues, and replies or fixes on issues you reported
- Pick what you get and how often (every 15 min to 1 hour); tapping a notification opens the title or issue

**Upcoming calendar**
- Month view and list of cinema, digital and disc releases and new episodes for your requests, everyone's requests, or your watchlist
- Add any date to your phone's calendar

**Admin**
- **Users**: search, sort, create local users, import from Jellyfin/Emby or Plex, edit permissions (one user or many), per-user quotas, delete
- **Server settings**: general, user sign-in options and default permissions/quotas, network (proxy, DNS cache, CSRF), Jellyfin/Emby/Plex connection with library toggles, library sync and full scans, Radarr and Sonarr servers (add, test, edit, delete), metadata providers, Tautulli, override rules, every notification agent (with test), discover sliders (enable, reorder, add, reset), jobs (run, cancel, reschedule) and caches (flush), logs with level filter and search, and about
- **Blocklist**: search and remove

**App**
- Tablet and foldable layouts: a side rail on wide screens, and tabs on the left with details on the right from 700 dp, split along the hinge on foldables
- Material You colours, light/dark/system theme, pure black, poster size
- Optional PIN app lock with fingerprint, auto-lock delay, and hiding content in screenshots and recents (on by default, server settings always hidden)
- Your session cookie is stored encrypted with an Android Keystore key

## Requirements

- Seerr, or Jellyseerr / Overseerr (the API is shared; a few newer features such as override rules and the blocklist need Seerr or a recent Jellyseerr)
- Android **13** or newer
- Your server address can be `http://192.168.x.x:5055`, a local hostname, or a reverse-proxy URL like `https://requests.example.com`

## Install

Download the latest `SeerrCtrl-vX.Y.Z-build.N.apk` from the [Releases](../../releases) page and open it on your phone. Android may ask you to allow installing apps from your browser or file manager.

Updates install over the existing app as long as every build is signed with the same release key.

## First run

1. Enter your Seerr address. Without `http://` or `https://`, the app tries https first and only offers plain http after warning that your password would be sent unencrypted.
2. Tick **Allow self-signed certificate** only if your server uses one. You'll be shown the certificate's SHA-256 fingerprint to confirm, and from then on only that exact certificate is accepted (only for your server; TMDB, Plex and others always use normal checks).
3. Sign in with any method your server offers. What you can see and do in the app follows your Seerr permissions, exactly like the web UI.

## Notifications on ColorOS and other battery-strict phones

The app checks your server in the background at most every 15 minutes (Android's minimum). Phones that limit background apps can delay or skip those checks. In **More → App settings → Notifications**, tap **Background activity** to let SeerrCtrl run unrestricted. On ColorOS you may also want to allow **Auto launch** for SeerrCtrl in the phone's app settings.

## Building from source

Open the project in Android Studio (Ladybug or newer) and run it, or build from a terminal:

```bash
./gradlew assembleDebug
```

Signed releases are built by GitHub Actions on every push to `main`. The workflow reads `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` from the repository's Actions secrets (`KEY_ALIAS` can be a repository variable). Without them the build fails rather than publishing a debug-signed release.

## Disclaimer

SeerrCtrl is not affiliated with the Seerr, Jellyseerr or Overseerr projects. Movie and series data comes from TMDB through your server.
