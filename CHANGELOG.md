# Changelog

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## 1.4.6

SeerrCtrl is now completely Google-free. Nothing changes in how you use it.

### Privacy

- **Emoji no longer make SeerrCtrl ask Google for a font.** A standard Android library inside the app asked Google Play services for a downloadable emoji font every time SeerrCtrl started, which could make Google Play services fetch that font from Google. This lookup is now switched off: emoji are drawn with your phone's own emoji font. On phones without Google Play services nothing changes; on other phones, emoji can look slightly different if the phone's built-in emoji font is older.
- **Checked: SeerrCtrl is Google-free.** It contains no Google Play services, Firebase, ML Kit, Google Cast or Play Core, and it doesn't load fonts or anything else from Google's servers. It also never switches server addresses by itself: it always uses the address you set up, and only you can change it.

### Changed

- **README: new Google-free line** under Requirements: works without Google Play services, contains no Google libraries that contact Google, and draws emoji with your phone's own emoji font.
- **README brought up to date with 1.4.5.** The feature list now also covers search for titles, people, keywords and studios; title pages with ratings, cast and crew, collections, recommendations and a Play button for your media server; your Watchlist and Profile; every kind of notification (your requests approved, declined, failed or available, issue replies, and new requests and issues for managers); quotas, the blocklist and logs in the admin screens; and a new "Private by default" line: the app is hidden from screenshots and recent apps unless you turn that off, lock-screen notifications hide titles and names, Approve/Decline from a notification needs the phone unlocked, and Gravatar and Plex profile pictures are off unless you turn them on. No change to the app itself.
- Tidied the changelog: removed a doubled bullet marker in the 1.4.5 notes.

## 1.4.5

Fourth security and privacy review, focused on what the app sends to sites other than your own Seerr server.

### Security

- **Studio and network logos respect Seerr's image cache.** When your Seerr admin turned on **Cache images locally**, every poster and backdrop already came through your Seerr server, but the studio and network logos on Discover were still loaded straight from TMDB. They now go through your server too
- **Notification posters respect Seerr's image cache.** When notifications were checked in the background with the app closed, the poster in a notification was always loaded straight from TMDB, even with image caching on. The background check now reads that setting from your server first
- **Profile pictures from Gravatar and Plex are off by default.** Other people's (and your own) profile pictures can be hosted by Gravatar or Plex; loading them told those sites your IP address and whose profiles you were looking at, for example on the Users and Requests screens. They now show the person's initial instead, unless you turn on **More > App settings > Profile pictures from Gravatar and Plex**. Pictures stored on your Seerr server (such as Jellyfin and Emby users' pictures) still load as before
- **Switching usage stats off takes effect straight away.** Turning off **App settings > Usage stats** now also throws away any stats still waiting to be sent and stops one being sent at that moment, and switching them on again starts a new random session

### Changed

- **Usage stats share less.** A request no longer says whether the advanced options were used or whether it came from a title or a collection, so the stats match exactly what the README promises: movie or series, and 4K or not
- **The README lists the other servers the app talks to:** TMDB's image server (unless your Seerr admin turned on image caching), plex.tv while signing in with Plex, and Gravatar and Plex for profile pictures if you turn them on
- **All Ctrl apps in the README.** The README's Ctrl series table now lists all 14 apps. New in the table: [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) (Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app), [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) (scan and browse documents on Paperless-ngx), [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) (manage your Proxmox VE server) and [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) (monitor and manage your TrueNAS server). Every app in the table is also in the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) for automatic updates.

## 1.4.4

### New

- **Optional anonymous usage stats.** After you sign in, SeerrCtrl asks once whether you'd like to share anonymous usage stats. Nothing is sent unless you tap **Share stats**, and you can switch it on or off any time in **More > App settings > Usage stats**
- **What's shared:** which screens and tabs you open (for example "Discover", "movie" or "Requests", never which movie, series, person or user), whether you request a movie or a series and whether in 4K, whether you approve or decline requests and how (button, swipe or notification), that you searched and which kind of search (all, keywords or studios), the app version, Android version, phone model, language, phone or tablet, and your theme setting
- **What's never shared:** your server address, username, passwords, which titles you request, approve or search for, what you type, TMDB or request IDs, or any ID that follows you between sessions. Each session gets a new random ID, and stats from before and after you switch them off and on again aren't linked
- **Where it goes:** to VKNXWorks' own self-hosted server (Aptabase, at telemetry.vkhub.org over HTTPS only), not to Google or any analytics company. Failed sends are dropped, never saved for later. Builds you make from source can't send stats at all, and the setting doesn't appear in them

### Changed

- **PrintCtrl in the Ctrl series list.** The README's table of Ctrl apps now includes [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl), the app for Bambu Lab printers through a self-hosted Bambuddy server, and Spoolman spools.
- The README has a new **Usage stats** section listing exactly what's collected

## 1.4.3

### Security

- Approve/Decline buttons on a "new request" notification now only work for the sign-in that posted them. Signing out cancels them for good, even if another app (for example one allowed to read your notifications) kept a copy of the button, so it can never approve or decline with an account you sign in to later
- "Forgot PIN? Log out" on the lock screen now leaves your account's screens immediately, instead of showing them for a few seconds while SeerrCtrl tells the server you've signed out

## 1.4.2

### Security

- Fingerprint unlock is now tied to a key in the phone's secure hardware that only a real fingerprint match can use, so a faked "success" can't open the app. Adding a new fingerprint to the phone turns fingerprint unlock off until you confirm it again. If you used fingerprint unlock before, the app asks for your PIN once; turn "Unlock with fingerprint" back on in App settings
- The wait after too many wrong PINs can no longer be skipped by changing the phone's date or time, and it carries on after a restart
- Wrong PINs typed when changing or turning off the PIN now count towards the same wait as on the lock screen
- Logging out (or your session ending) now removes SeerrCtrl's notifications, so their Approve/Decline buttons can't act on a later sign-in, stops all background checks, and clears cached posters, avatars and title details from the phone
- Approve/Decline from a notification does nothing if you've signed out since it arrived
- Account settings (passwords, Pushbullet and Pushover tokens) are always hidden from screenshots and the recent-apps screen, like server settings
- Password fields in account settings and "Create local user" use a password keyboard, so the keyboard doesn't learn or suggest what you type
- Links from the server (play, trailer, ratings, Radarr/Sonarr pages, "Forgot password?") only open if they're normal web addresses (http or https); anything else is ignored

### Fixed

- Tapping Back (or a list item) quickly several times in a row could close one screen too many and leave a black screen. Repeated taps while a screen is still opening or closing are now ignored, and the first screen is never closed
- Saving or deleting a Radarr/Sonarr server, an issue or a user no longer closes an extra screen if you had already gone back
- Plex sign-in no longer stalls when plex.tv hands out a very large sign-in number

## 1.4.1

- Version numbers are now plain, with no build number on the end, and a new version only comes out when something in the app actually changes. Nothing else in the app has changed.

## 1.4.0

- New app icon in the Ctrl series keycap style (like JellyCtrl and NaviCtrl): a looking glass on the key face in lavender, with matching themed and notification icons

## 1.3.1

- Security hardening:
  - Self-signed certificates are now trusted by fingerprint: you confirm the server's SHA-256 once and only that certificate is accepted, only for your server. If you had the option on, the app asks you to confirm the fingerprint
  - Addresses without `https://` try https first; plain http is only used after a warning that it's unencrypted, and a "Not encrypted (HTTP)" note shows in App settings
  - The session cookie is only sent to your exact server (scheme, host and port), also across redirects; plex.tv and image hosts must use https
  - Approve/Decline from a notification needs the phone unlocked; titles, names and comments are hidden on the lock screen
  - "Hide content in screenshots & recents" works without an app PIN and is on by default; server settings screens are always hidden
  - Passwords, tokens and webhook URLs in server notification settings are masked
  - Notification links can't be faked by other apps; nothing is included in backups or device transfers
  - Fingerprint unlock uses strong biometrics only (biometric 1.2.0-alpha05)
  - CI: releases are never debug-signed, actions pinned to commits, release published from a separate least-privilege job

## 1.3.0

- Floating tab bar: on phones the bottom bar is now a frosted-glass island that blurs the content scrolling behind it, with a selection bubble that springs between tabs

## 1.2.0

- New app icon: an eye with a looking glass for a pupil, in Seerr's indigo, with a matching themed (monochrome) icon and notification icon

## 1.1.2

- Fixed the "Recently changed" chip wrapping onto four lines on the Requests tab; filter rows on Requests, Issues and Search now scroll sideways

## 1.1.1

- Much smaller app: release builds are now shrunk and optimised (R8 + resource shrinking)

## 1.1.0

- **Notifications**: your requests approved, declined, failed or available; new requests to approve with Approve/Decline buttons right in the notification; new issues, and replies or fixes on issues you reported. Choose what you get and how often (15 min–1 h) in App settings, where you can also let the app run in the background so ColorOS doesn't delay checks
- **Swipe to approve or decline**: swipe a pending request right to approve, left to decline
- **Upcoming calendar** (More → Upcoming): month view and list of cinema, digital and disc releases and new episodes for your requests, everyone's requests or your watchlist; add any date to your phone's calendar
- **Tablets and foldables**: side navigation rail on wide screens; from 700 dp the tabs stay on the left and details open on the right, split along the hinge on foldables
- Signing values can be repository variables or secrets

## 1.0.0

First release.

- Sign in with Jellyfin/Emby (and Quick Connect), Plex, or a Seerr account
- Discover sliders, browse with filters, search
- Movie and series pages with requests (seasons, 4K, advanced options), trailers, play links, issues, watchlist, ratings, seasons and episodes, cast, collections
- Requests and issues tabs with approve/decline, comments and resolving
- Profiles, watchlists, account settings, notification settings
- Admin: users and permissions, server settings, services, notifications, discover sliders, jobs and caches, logs, override rules, blocklist
- App lock, Material You, pure black theme
