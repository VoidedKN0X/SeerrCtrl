# Changelog

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

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
