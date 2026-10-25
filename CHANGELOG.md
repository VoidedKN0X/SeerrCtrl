# Changelog

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## 1.9.0

Instant notifications through ntfy, set up from the app, and an "Open in ArrCtrl" button on movie and series pages.

### New

- **Instant notifications with ntfy.** In **Settings › Notifications** (tap your profile picture), **Instant notifications** lets you pick **ntfy** instead of **Off (background checks only)**. SeerrCtrl then registers with the ntfy app (UnifiedPush) and gets its own push address on your ntfy server. Seerr's Webhook agent posts to that address the moment something happens, ntfy wakes SeerrCtrl, and SeerrCtrl asks your Seerr server for the details and shows the same notifications as the background check: your request approved, declined, failed or available, new requests waiting for approval with **Approve** and **Decline** buttons, new issues, replies on issues and issues resolved or reopened. ntfy is the only push app SeerrCtrl uses; nothing goes through Google.
- **Background checks stay on as the fallback.** The checks every 15, 30 or 60 minutes keep running with push on. Pushes and checks share what was already notified, so each event is only shown once, whichever arrives first, and nothing is missed if a push gets lost.
- **Push status line.** Shows whether push is off, waiting for ntfy, on (with when the last push arrived), whether ntfy's server can't be reached for now, or why ntfy refused to register SeerrCtrl. If ntfy isn't installed there's an **Install** button (F-Droid), and if ntfy is battery-restricted a row opens its App info so you can set Battery to Unrestricted.
- **Set up Seerr's webhook.** A new page shows exactly what to enter in Seerr's Webhook agent: the webhook URL (your push address), an empty Authorization header, the JSON payload to paste, and which notification types to tick. The URL and payload copy with one tap and are kept out of clipboard previews, because anyone with the push address can wake the app.
- **Set up automatically.** If your account may change Seerr's settings, **Set up Seerr's webhook for me** turns on Seerr's Webhook agent with your push address, the payload and every request and issue type, after you confirm. If the agent already posted somewhere else, the confirmation names that address, since Seerr's webhook can only post to one.
- **Send a test push.** Posts to your push address like Seerr will; when it comes back through ntfy, a "Push works" notification shows. A test sent from Seerr's own Webhook page shows the same notification.
- **Open in ArrCtrl.** Movie and series pages get a button that opens the title in ArrCtrl (Radarr or Sonarr), with the TMDB id for movies and the TVDB and TMDB ids for series. It only shows when ArrCtrl is installed; an older ArrCtrl without the link just opens.

### Privacy and security

- **Pushes carry no titles or names.** The payload only says what kind of event happened and the request or issue number, so no titles, usernames or comments pass through your ntfy server. SeerrCtrl looks those up on your Seerr server itself, so a forged push can at most make the app check your server early.
- **Only ntfy can talk to SeerrCtrl's push receiver.** Every message must carry the secret token of SeerrCtrl's registration, and on Android 14 and newer a sender that identifies as any other app is ignored.
- **Signing out drops the push address.** The registration with ntfy is removed, so a webhook left on that server can never wake the app again. Pick ntfy again after signing in for a new address.

## 1.8.0

Light mode that follows the device theme: SeerrCtrl is now light when your phone is in light mode and dark when it's in dark mode, switching straight away when you change it.

### New

- **Light mode that follows the device theme.** SeerrCtrl follows Android's dark theme setting, including scheduled switching (sunset to sunrise, bedtime). There's still no theme setting in the app, and still no wallpaper colours: it simply matches your phone. Switching while the app is open changes it right away, without restarting the app or losing what's on screen.
- **Signal Light palette.** In light mode the background is a warm off-white (#F5F5F1), cards, sheets and dialogs are white with a thin light grey border, text is near-black, and secondary text is a mid grey. The dark mode from 1.7.0 is unchanged.
- **Same violet accent, readable on white.** Filled accent areas keep the light violet (#B48CFF) with dark content in both modes: the Discover hero card, the Request, Play and other main buttons, the selected tab, switches that are on, selected filter chips, the profile picture on the You page and the PIN cells. Accent text and icons on a light background (the slash in SEERR/CTRL, "See all", ■ AVAILABLE and ■ OPEN status words, the watchlist icon when a title is on your list, the outline of a text field you're typing in, the fingerprint key on the lock screen) use a deeper violet (#6A3BDB) in light mode so they stay easy to read.

### Changed

- **Status and navigation bars** match the background in both modes, with light icons in dark mode and dark icons in light mode. Before the app has drawn, the window is already the right colour, so there's no dark flash in light mode or light flash in dark mode.
- **Every screen and part works in both modes:** the tabs and the floating tab bar (white at about 92 % with a light border in light mode), movie and series pages, request and issue lists, swipe to approve or decline, settings and the You page, dialogs and bottom sheets, text fields, switches (light grey track with a grey knob when off), progress bars (light track with a near-black fill), the calendar, the lock screen and the PIN pad (white keys with a light border, light grey empty PIN cells). Errors use a deeper red (#D93636) on light backgrounds.
- **Studio and network logos** on Discover stay on dark tiles in light mode too, because the logos themselves are white.

## 1.7.0

SeerrCtrl gets the new Ctrl / Signal look shared by the whole Ctrl series: near-black, one violet accent, Space Grotesk and JetBrains Mono. Everything works as before; only the look changed.

### Changed

- **New dark look, everywhere.** The app now uses one fixed dark palette: a near-black background (#08080A), dark grey cards and sheets (#141417) with a thin border, light text (#F4F4F2), muted grey for secondary text, and one violet accent (#B48CFF) for what matters most (the selected tab, the main button, switches that are on, available titles). The status and navigation bars match the background, edge to edge, with light icons.
- **New fonts, built into the app.** All text is set in Space Grotesk; numbers, dates, server addresses, section labels and the SEERR/CTRL name use JetBrains Mono. Both fonts are bundled (SIL Open Font License 1.1, licence texts included in the app), so nothing is fetched from Google or anywhere else.
- **Top of every tab.** Discover, Search, Requests, Issues and More now show the **SEERR/CTRL** wordmark at the top left (the slash in the accent colour) and your profile picture at the top right as a 44 dp rounded square with your initials or Seerr avatar. A small accent square on its corner shows you're signed in. The tab's name sits underneath as a large bold headline. Tapping the picture still opens the You page, which is still the only way into Settings.
- **Discover hero card.** The top of Discover has a big accent card with one number: requests waiting for approval if you manage requests, or the number of requests made from your account otherwise. Tap it to open the Requests tab. Pull to refresh updates it too.
- **Posters and rows.** Posters have 14 dp rounded corners and a thin border. The Movie/Series tag is a small dark mono label and availability is a square accent dot in the corner. Row titles are plain semibold text, "See all" is a small mono accent link, and genre, studio and network tiles use the dark card style.
- **Status in words, not coloured pills.** Request, availability and issue states are now shown as mono capitals with a small square dot ("■ AVAILABLE", "■ PENDING"): accent for available and open, light for approved and processing, grey for pending and resolved, red for declined and failed.
- **Movie and series pages.** The back button is a 44 dp rounded square. **Request** and **Play** are large accent buttons, the 4K request, trailer and other actions are dark outlined buttons, and the watchlist, report and manage actions are square buttons. Ratings, the facts table (with mono labels), seasons (as plain rows with "S01"-style numbers), downloads (thin 6 dp progress bars), issues and keywords all follow the new style.
- **Request and issue lists.** Cards use the new dark card style with 22 dp corners and a thin border. Swiping a pending request shows "APPROVE" or "DECLINE" in mono capitals. Filter chips are rounded rectangles with the accent fill when selected.
- **Floating tab bar.** The bottom bar is a dark rounded bar floating above the navigation bar. The selected tab is an accent block with its icon and name; the other tabs show only their icon. The frosted blur behind the old bar is gone, and lists get enough room at the bottom so nothing hides behind the bar. On tablets and unfolded phones, the side rail uses the same accent selection.
- **Detail screens.** Every screen opened from a tab has a square back button and its title in Space Grotesk, without a coloured app bar.
- **Switches.** All switches are rounded rectangles: an accent track with a dark knob when on, a dark grey track with a grey knob when off. TalkBack still reads them as switches.
- **Lock screen.** The lock screen shows an accent rounded square with a lock, "VKNXWORKS · CTRL", **Locked** in large type, square PIN cells (filled in the accent colour as you type) and a three-column keypad with large rounded keys in JetBrains Mono. Fingerprint unlock is the accent-outlined key at the bottom left of the keypad (it replaces the "Use fingerprint" button under it), and delete is the key at the bottom right. "Forgot PIN?" stays under the keypad. The same keypad is used to set, change or turn off the PIN. How the lock works hasn't changed.
- **You page.** Your picture is a 68 dp accent rounded square with your initials, next to your name and the address of your Seerr server in mono type. Settings, Server and About are grouped in dark cards with mono section labels and thin dividers between rows. **Support SeerrCtrl** is now a large accent button at the bottom, above the version line and "GOOGLE-FREE · TELEMETRY-FREE · ARM64".
- **Settings.** Groups have mono section labels ("PRIVACY AND SECURITY", "NOTIFICATIONS") and dark cards with thin dividers; row icons are plain outlined icons instead of tinted circles. The settings themselves (app lock, Hide in screenshots, Hide in recent apps, notifications and the rest) are unchanged.
- **Dialogs, sheets and text fields.** Dialogs and bottom sheets are dark cards with 26 dp corners and a thin border. Text fields are dark with a thin border and 16 dp corners, and get an accent border and cursor while you type. Messages at the bottom of the screen use a dark rounded box.

### Removed

- **Theme choice and wallpaper colours.** The Signal look is dark only, so **Settings › Appearance › Theme** (Follow system, Light, Dark, Pure black) and **Use wallpaper colours** are gone. Whatever you had picked before, SeerrCtrl now always uses the new dark look. Poster size stays.

## 1.6.5

### Fixed

- **SeerrCtrl asks for notification permission at the right time.** On a new install, Android's "Allow notifications?" question now comes up once SeerrCtrl's own screens are on show: after you've signed in, unlocked the app and finished setting a PIN. Before, it could be asked while the lock screen or a PIN screen was showing, where the question could be hidden or dropped, and then never came back, so request and issue notifications were silently never shown. It's still only asked automatically once, and not at all if you've turned notifications off in SeerrCtrl.
- **Settings shows when notifications are blocked.** If Android isn't letting SeerrCtrl show notifications (you tapped "Don't allow", or turned them off in Android's settings), **Settings → Notifications** now shows a **Notifications are blocked** row with an **Allow** button, and the Notifications switch shows as off instead of looking on. Tapping **Allow** (or the switch) asks Android again, or, once Android won't ask any more, opens SeerrCtrl's notification settings so you can turn them on there. Coming back to the app updates the row straight away, without leaving and reopening Settings.
- **Notifications switched off in Android's settings are recognised.** SeerrCtrl used to count notifications as allowed when the permission was granted even if you had turned all of its notifications off in Android's settings. Both are now checked.

## 1.6.4

### Fixed

- **Fingerprint unlock works again after you leave the app.** Once you'd left SeerrCtrl with the back gesture or by swiping home, the lock screen no longer asked for your fingerprint when you came back, and tapping **Use fingerprint** could do nothing either; only the PIN worked until the app was force-closed. The fingerprint prompt was asked for while SeerrCtrl was still going to the background or coming back on screen, the phone dropped it without answering, and the lock screen never asked again for that lock. Now the prompt only opens once SeerrCtrl is fully back on screen, a prompt that never answered can't block the next one (tapping **Use fingerprint** always opens a fresh one), and the lock screen offers your fingerprint again every time you come back to it. The same applies to the prompt for turning on fingerprint unlock in **Settings**.

## 1.6.3

### Privacy

- **No hidden dependency list in the APK**: Android's build tools used to tuck an encrypted list of the app's libraries, readable only by Google, into the APK's signature block. It's no longer added, so the APK holds nothing that only Google can read and passes F-Droid's check for it. Nothing changes in the app itself.

## 1.6.2

### Changed

- **Tab titles match the other Ctrl apps**: the big titles on Discover, Search, Requests, Issues and More are now the same size and weight as in ProxCtrl, NaviCtrl and the rest of the series (slightly larger, regular weight instead of bold), with the same spacing around them and your profile picture in the same place.
- **Whole sentences instead of joined pieces, ready for translations**: calendar entries ("S2E5 · episode name", "In cinemas · note"), issue notifications ("Alex: reported a problem"), who requested something and when, discover slider details, the server line on the You page and the " · " between details are now each one piece of text. The wording stays the same.

## 1.6.1

### Changed

- **All text is now in one place, ready for translations.** Over 700 pieces of text that were written straight into the code (Discover, Search, Requests, Issues, media and people pages, the request and report dialogs, server settings, users, the calendar, sign-in, certificate warnings, notifications and their channels, and error messages) now come from the app's text resources. The wording stays the same.

### Fixed

- **Counts read correctly in the singular and plural**: "1 season", "1 episode", "1 user", "1 movie", "1 request", "1 comment", "Request 1 season" and "Permissions for 1 user" instead of "1 seasons", "1 users" and so on, in the app and in notifications.

## 1.6.0

Settings now open from your profile picture, the same way as in every other Ctrl app.

### Changed

- **Settings moved to your profile picture**: your profile picture now sits at the top right of every tab (Discover, Search, Requests, Issues and More). Tap it to open the new **You** page. Before, settings were under **More › App settings**, and that row is gone, so the profile picture is now the only way in.
- **The You page** shows your picture, name, account type and email at the top, then **Profile** (your requests, quota and stats) and **Account settings** (name, region, notifications and password on your Seerr server), the **Server** you're signed in to (address, Seerr version, and **Trusted certificate** when you use a self-signed certificate), then **Settings** and **Log out**, and the **About** section with the version at the bottom. Profile, Account settings, the server details, Log out and About used to be on the More tab or at the bottom of App settings.
- **Your profile picture** is your Seerr avatar. Pictures from Gravatar and Plex still only load when **Profile pictures from Gravatar and Plex** is on; otherwise your initial shows, on the same tinted circle as in the other Ctrl apps.
- **App settings is now called Settings** and holds only the settings themselves, in the series order: **Appearance**, **Notifications**, **Privacy and security** and **Browsing**. Nothing in them changed.
- **The More tab** keeps its real features: **Watchlist** and **Upcoming**, and for managers **Users**, **Blocklist** and **Server settings**. Your name and picture at its top moved to the You page.
- **The Search tab** now has a "Search" title like the other tabs, with the profile picture next to it, above the search box.
- **README**: Getting started says what's behind the profile picture, and every path to a setting now starts with "tap your profile picture".

## 1.5.4

### Changed

- **Hide in screenshots** and **Hide in recent apps** are now two separate switches in More › App settings › Privacy and security, both off by default, the same in every Ctrl app. Hide in screenshots makes screenshots and screen recordings show a blank screen; Hide in recent apps shows a blank card in the recent-apps view instead of the app (screenshots still work). Before, one setting, "Hide in screenshots and recent apps", blocked screenshots and was on unless you turned it off, and the recent-apps view always showed a blank card. **What changes for you:** if you had turned that setting on yourself, both new switches are on. If you never changed it, both are now off, so screenshots work and the recent-apps view shows SeerrCtrl; turn them on if you want the old behaviour. With the app lock on, the recent-apps view always shows a blank card.
- **Always protected, whatever the switches say:** sign-in, the lock screen, the PIN screens and the server settings (with their API keys and passwords) are never in screenshots, screen recordings or casting.
- The app lock's text and the Privacy and security rows now come from the app's translatable text resources, with the same wording as the other Ctrl apps. Nothing else changes in how they work.
- **README**: the App lock section now uses the same wording as the other Ctrl apps and describes the two switches.

## 1.5.3

### Security

- **Sign-in, the lock screen and the PIN screens are never in screenshots, screen recordings or casting**, whatever "Hide in screenshots and recent apps" is set to. This covers the sign-in screen where you type your password or Plex and Quick Connect details, the lock screen, and choosing, changing or confirming your app PIN. A recording would otherwise show which keys of the PIN pad were pressed.

## 1.5.2

The app lock and App settings now look and work the same as in every other Ctrl app.

### Changed

- **New lock screen look**, the same as in the other Ctrl apps: a round lock badge, PIN dots that show a spinner while your PIN is checked, and **Forgot PIN?** (it still asks first, then logs out and resets the lock).
- **Turning the app lock on, changing the PIN and turning the lock off now use the same full-screen PIN pad** as the lock screen, instead of a dialog with text boxes. Choosing a PIN asks for it twice; changing it asks for your current PIN first; turning the lock off asks for your PIN.
- **App settings are grouped like in the other Ctrl apps**: **Appearance**, **Notifications**, **Privacy and security**, **Browsing**, **Server** and **About** at the bottom. Rows have the same look as the other apps, and choices such as Theme, Poster size, Lock again and Check every open a list to pick from. The same look applies to the More tab and the server settings.
- **Privacy and security** (formerly "Security") has the same rows, wording and icons as every Ctrl app: **App lock**, **Change app PIN**, **Unlock with fingerprint**, **Lock again** and **Hide in screenshots and recent apps** (formerly "Hide content in screenshots & recents"). **Profile pictures from Gravatar and Plex** moved here from Browsing, and a note under the section says what SeerrCtrl connects to.
- **Theme choices are the same as in the other Ctrl apps**: Follow system, Dark, Light or **Pure black**. Pure black is now one of the themes instead of a separate switch: if you had Dark with pure black on, you now get the Pure black theme; with "System" and pure black on, SeerrCtrl follows the system with the normal dark colours (pick Pure black again if you want it). "Material You colours" is now called **Use wallpaper colours**.
- **The fingerprint prompts** now say what they're for ("Confirm it's you to see your requests" when unlocking, "Confirm your fingerprint" when turning fingerprint unlock on).
- **README**: a new App lock section that explains the PIN, fingerprint unlock, the wrong-PIN wait, Lock again, hiding the app in screenshots and recent apps, how notifications work while locked, and Forgot PIN. The Ctrl series table now lists AdCtrl and MemoCtrl.

### Security

- **Open dialogs and menus are covered by the lock.** When SeerrCtrl locked while a dialog, menu or sheet was open (a request, an issue report), that window could stay on top of the lock screen. The lock screen is now a full-screen window of its own above everything else in the app.

## 1.5.1

### Security

- **Trips out of the app no longer skip the lock if you stay away.** Links, trailers, the calendar and Android's settings and permission prompts still don't count as leaving the app, but if you don't come back within 5 minutes (or your "Lock again" time, if that's longer), SeerrCtrl locks anyway. Before, such a trip never locked, however long you were gone.

## 1.5.0

The app lock now works the same way in every Ctrl app, with the strongest protection of the options we compared.

### Changed

- **PINs now need at least 6 digits** (up to 12), and very obvious ones such as 111111 or 123456 are refused. If your PIN has 4 or 5 digits, it still unlocks SeerrCtrl once after updating, and then you choose a new one before going on.
- **Fingerprint unlock has to be turned on again** after updating (**More > App settings > Unlock with fingerprint**), because it now unlocks a different, stronger key. The PIN is also needed instead of the fingerprint after the phone restarts and at least every 72 hours, the same as Android does for your screen lock.
- **"Lock again" has more choices**: as soon as you leave the app, after 1, 5 or 15 minutes away, after 1 hour away, or only when the app restarts. "Instantly" stays as "As soon as you leave the app"; "30 s" becomes "As soon as you leave the app", so nothing waits longer than before. New setups start with "As soon as you leave the app".
- **Opening a link, a trailer, a calendar app or Android's settings from SeerrCtrl no longer counts as leaving the app.** Coming straight back doesn't ask for the PIN again; going to the home screen or another app yourself still does.
- **What you were doing is still there after unlocking**: a half-written request or issue report stays open underneath the lock instead of being closed.
- **"Forgot PIN? Log out"** now asks first, explains that the PIN can't be recovered, and turns the app lock off along with logging out.

### Security

- **Your sign-in is encrypted with your PIN while SeerrCtrl is locked.** The key that decrypts it only exists in memory while the app is unlocked and is wiped the moment it locks, so a locked app, or a copy of its files, can't use your Seerr session. **Exception:** while notifications are on, background checks need the session when the app isn't open, so it stays encrypted only with the phone's Keystore, as before. Turn notifications off for the full protection; the App lock setting says which applies.
- **The PIN can't be guessed off the phone.** It's never stored. It's stretched with 600,000 rounds of PBKDF2 (was 120,000) and mixed with a secret held in the phone's secure hardware (StrongBox where available), so copying SeerrCtrl's files to a computer isn't enough to try PINs there.
- **Fingerprint unlock can't be faked**: the fingerprint unlocks the key that opens your sign-in, inside the phone's secure hardware, so a tampered "success" on a rooted phone opens nothing. Adding a new fingerprint turns fingerprint unlock off until you turn it on again.
- **Wrong PINs still lock the screen** (30 seconds after 5 tries, doubling up to 15 minutes). Restarting the app or the phone, or changing the clock, doesn't reset the wait, and a wait running when you update carries over.

### Changed (README)

- **README**: the App lock line describes the new PIN, fingerprint and encrypted sign-in

## 1.4.12

### Changed

- **64-bit ARM phones only.** SeerrCtrl now ships only the 64-bit ARM (arm64-v8a) build that nearly every phone from the last several years uses, like every Ctrl app, so the download is smaller. Old 32-bit phones and x86 devices (emulators, some Chromebooks) are no longer supported.
- **README**: Requirements say 64-bit ARM phones only

## 1.4.11

### Privacy

- **Google-free and de-Googled.** SeerrCtrl has no Google Play services, Firebase or other Google services and never contacts Google. This release removes the last mention of Google from inside the app (a note in the app's setup about emoji fonts), so nothing Google-related is left in it. Nothing changes in how the app works: emoji still use your phone's own emoji font, and SeerrCtrl works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps)

### Changed

- **README**: the "Google-free" line now reads "Google-free and de-Googled" and names the de-Googled phones SeerrCtrl works on

## 1.4.10

### Privacy

- **No more usage stats: SeerrCtrl is now telemetry-free.** The optional anonymous usage stats are gone completely, along with the Usage stats setting in More > App settings and the one-time question after signing in. The app no longer contacts VKNXWorks' stats server at all, and the settings it used are deleted from your phone when you update. SeerrCtrl now only talks to your Seerr server, plus TMDB's image server, plex.tv and Gravatar in the same cases as before (see Privacy in the README)

### Changed

- **README**: a Screenshots section (screenshots are coming soon), a new Privacy section and a "Telemetry-free" badge, and a link preview image (`.github/assets/social-preview.png`) with SeerrCtrl's icon, name and summary, for when the repo is shared on Reddit, Discord or elsewhere

## 1.4.9

### Changed

- **One About section in every Ctrl app**: Support, Report a bug, Source code and Licence (GPL-3.0), with the version underneath, at the bottom of More > App settings
- New **Report a bug** row opens SeerrCtrl's GitHub issues, where you can report a problem or suggest an idea
- New **Source code** row opens SeerrCtrl's GitHub repository, and **Licence** opens the GNU General Public License v3.0 that SeerrCtrl is released under
- The version row in About is replaced by a small "SeerrCtrl <version> · VKNXWorks" line under the section, and every About row now shows an arrow to show it opens a page

## 1.4.8

### New

- **Support SeerrCtrl** is now the first row under **About** in **More > App settings**. Tapping it opens VKNXWorks' GitHub Sponsors page (in the in-app browser tab if you've turned on **Open links in the app**, otherwise in your browser). SeerrCtrl is free and open source, and sponsoring helps keep it that way

## 1.4.7

### New

- **Open in SeerrCtrl from ArrCtrl.** The "Open in SeerrCtrl" button on a movie or series in [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) now opens that movie or series straight away in SeerrCtrl, ready to request or check, instead of just starting the app. Other apps can do the same with a `seerrctrl://movie/<TMDB id>` or `seerrctrl://tv/<TMDB id>` link. Such links can only open a movie or series page; web pages can't use them

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
