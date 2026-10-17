# Changelog

All notable changes to **Boom, listener!** are recorded here, by version. This file is also the text of each GitHub Release, so it is written in English, for people who use the app.

Format inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/) — `MAJOR.MINOR.PATCH`.

## [0.6.2] — 2026-10-17

### 🐛 Fixes
- **No more flicker when you swipe the mini player away.** For a split second it used to show an empty song ("Unknown artist", blank cover) before disappearing; now it slides down and fades out still showing the song you closed.

## [0.6.1] — 2026-10-17

### 🐛 Fixes
- **Swiping the mini player down now really stops the music.** Sometimes it only hid the mini player while the song kept playing.
- Swiping the mini player up now opens the player exactly as fast as your finger moves, even the first time.

## [0.6.0] — 2026-10-17

### ✨ New
- **Gestures for the player**: swipe the mini player up to open the full player, swipe the full player down to close it, and swipe the mini player down to stop playback. Everything follows your finger and finishes with a smooth spring.
- **Predictive back on the player**: the system back gesture shrinks the player as you swipe, and closes it when you let go (or puts it back if you change your mind).

### 🐛 Fixes
- **Smooth transitions everywhere.** Opening and closing screens now slides gently, and switching tabs fades. Before, the system back gesture could show a broken, half-drawn screen, because the player was a regular page and the bottom bars popped in and out.

## [0.5.0] — 2026-10-17

### ✨ New
- **English and Portuguese**: the app now speaks English by default and Portuguese when your phone is set to Portuguese.
- **Check for updates** in Settings → About: looks for a newer version on GitHub and offers to download it. It only contacts GitHub when you tap it.

### 🐛 Fixes
- **The progress wave now moves from the very first second.** Before, the bar stayed flat during the first 10% of the song and the last 5%, which looked frozen.
- Songs without a title tag now show their file name.

## [0.4.0] — 2026-10-17

### ✨ New
- **The app plays music!** Tap a song in the Library and it starts playing; the list on screen becomes the queue (while searching, only the search results).
- **Mini player** above the navigation bar, with cover, song, artist, previous, play/pause and next. Tap it to open the full screen.
- **Now Playing screen**: big cover, a wavy progress bar (flat when paused) you can drag, elapsed and total time, shuffle, previous, play/pause, next and repeat (all, one, off).
- **Keeps playing with the app closed**, with controls in the notification, on the lock screen and on your headphones. Pauses on its own when headphones disconnect or a call comes in.
- **Reorganized Settings** in sections (General and About), with colored icons and a large title.
- **Appearance**: follow system, light, dark or AMOLED (pure black), and turn wallpaper colors on or off.
- **About, Privacy and Licenses** in Settings.

### 🐛 Fixes
- **Test builds now update in place**, no uninstalling needed.

## [0.3.0] — 2026-10-17

### ✨ New
- **Song list**: the Library shows every song on your device, alphabetically, with cover, artist and album. Ringtones, alarms and recordings are left out.
- **Search**: the magnifier at the top of the Library finds songs by title, artist or album, ignoring accents and capitals.
- **Per-song menu** (three dots): go to artist, edit tags and see the duration.
- **Wellbeing moved**: it left the bottom bar and now lives in **Settings → Wellbeing**.

### 🎨 Visual tweaks
- Every screen got a top bar with a title, and inner screens got a back arrow.

## [0.2.0] — 2026-10-17

### ✨ New
- **Access to your music**: the Library explains why the app needs access to music and audio and asks for it. If denied twice, a button takes you straight to Android's settings.

## [0.1.0] — 2026-10-17

The first version: the app's skeleton, not playing music yet.

### ✨ New
- App structure with the **Library**, **Releases**, **Wellbeing** and **Settings** tabs.
- Placeholder **Artist**, **Tag editor** and **Now Playing** screens.
- Material 3 Expressive look, using your wallpaper colors on Android 12 and newer.
