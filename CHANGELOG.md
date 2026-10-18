# Changelog

All notable changes to **Boom, listener!** are recorded here, by version. This file is also the text of each GitHub Release, so it is written in English, for people who use the app.

Format inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/) — `MAJOR.MINOR.PATCH`.

## [0.19.0] — 2026-10-17

### ✨ New
- **Your profile.** Tap your initial in the top right corner of any tab (or your name in Friends):
  - A header tinted with the cover of your most played song, with your name, @ and a quick switch for sharing what you listen to.
  - **This month**: how many songs, how long and how many artists you listened to.
  - **Most played** with their covers and ranking, **listening time** for the last 7 days, your **peak hours** (morning, afternoon, night, late night) and **your artists** of the last 30 days.
  - Your recent **history** (Today, Yesterday…) and the artists you **follow**.
  - Edit your name right there. Everything is calculated on your phone.

## [0.18.1] — 2026-10-17

### 🐛 Fixes
- **Creating a Friends profile no longer says every @ is taken** when the server can't be reached. It now says it couldn't connect.

## [0.18.0] — 2026-10-17

### ✨ New
- **Friends tab.** Create a profile with just a name and an @ (no email, no password) and:
  - **Chat** with anyone by their @, or in **groups** you invite people to with a code.
  - **Recommend songs** from any song's ⋮ menu (library, playlists, albums and the player) or from the ♪ button in a chat. Only the song's info goes (title, artist, cover, YouTube Music link), never the audio file. If your friend has the same song, they can play it right there.
  - **See what friends are listening to**, and their recent songs, if they choose to share it. Sharing is off until you turn it on, and only people you talk to can see it.
  - Conversations are saved only on your phone. The server just delivers each message and deletes it.
- **Friends and sharing** in Settings: turn sharing on or off, or delete your profile.

## [0.17.0] — 2026-10-17

### ✨ New
- **Pick up where you left off.** Close the app and open it again: the mini player comes back with the same song, the same queue and the same spot in the song, paused. If you had ended the music (swiped the mini player down), the app opens clean.

### 🎨 Visual tweaks
- **Rounder corners** on the ⋮ menus and on the top of the player while it slides up or down.

## [0.16.1] — 2026-10-17

### 🎨 Visual tweaks
- **The tab bar only shows on the three tabs.** Artist, album, playlist and genre pages, the tag editor, history and Settings now use the whole screen.

### 🐛 Fixes
- **Releases no longer goes back decades.** It now shows what came out in the **last 6 months** by default, with pills for the **last month** or the **last year**. An artist's full discography is still on their profile.

## [0.16.0] — 2026-10-17

### ✨ New
- **The Releases tab works.** See the albums, EPs and singles of the artists you follow, either **Latest** (newest first, across everyone) or **By artist**. Filter by Albums, EPs or Singles; anything from the last 30 days gets a **New** tag. Tap a release to open its album page, or an artist's name to open their profile.
- **Tap the tab you're on to jump back to the top** of its list. Inside a screen of that tab (like an album), it takes you back to the tab's start.

### 🎨 Visual tweaks
- **Settings is now a focused screen**: the mini player and the tab bar slide away while you're in Settings (and Wellbeing), and come back when you leave.
- **The tag editor hides the mini player**, so the form has the whole screen.

### 🐛 Fixes
- **Only one tag editor at a time.** Opening tags for another song replaces the editor that was open instead of stacking them.

## [0.15.1] — 2026-10-17

### 🎨 Visual tweaks
- **Library sections are now pill buttons** (Genres, Artists, Playlists, Albums) at the top, instead of a carousel of cards. The selected one is filled and shows a check.

## [0.15.0] — 2026-10-17

### ✨ New
- **New Library tab.** A carousel at the top switches between **Genres**, **Artists**, **Playlists** and **Albums**, each card showing a cover from that section. Open a genre or an album to see its songs with **Play** and **Shuffle**; artists open their profile.
- **Playlists.** Create, rename and delete your own playlists. Add songs from any song's ⋮ menu (library, playlists, albums, genres and the player), remove them, and hold the handle to reorder. They stay on your phone.
- **Listening history.** Every song you listen to for 30 seconds or more, grouped into **Today**, **Yesterday**, **Earlier this week**, **Last week** and past months, with the time you heard it. Tap a song to play that group from there. You can clear it anytime.

### 🎨 Visual tweaks
- **Settings and History moved to the top right corner** of every tab. The bottom bar is now **Home** (your songs), **Releases** and **Library**.

## [0.14.1] — 2026-10-17

### 🐛 Fixes
- **Sharing now sends the song's own YouTube Music link** (`music.youtube.com/watch?v=…`), which opens straight in the app of whoever receives it. The song is matched on Deezer and converted with song.link; if no match is found, the YouTube Music search link is shared instead. Works for songs in the library, the player, artist pages and album pages, and for albums too.

## [0.14.0] — 2026-10-17

### ✨ New
- **Tag editor.** Open it from a song's menu (library or player) to edit title, artist, album, album artist, year, track and disc number, genre, composer, lyrics and the cover. Changed fields are marked, and you're asked before leaving with unsaved edits. Works with MP3, FLAC, M4A, OGG, OPUS and more.
  - **Change the cover** with the Android photo picker. No access to your photos is needed.
  - The first time you save a song, Android asks you to allow the change to that file.
  - Saved changes show up right away in the library, the player and the notification, without stopping the music.
- **Fix tags automatically.** One tap looks up the song on Deezer and fills in the official title, artist, album, year, track number and cover, with a more precise genre from Last.fm. When it's not sure, it shows the closest matches for you to pick. You review everything and can undo before saving.
- **Lyrics saved in the file** now show first in the player, even offline. Synced (LRC) lyrics work too.

### 🎨 Visual tweaks
- **Smoother screens.** Inner screens slide in over the previous one and slide back out, and the back gesture follows your finger all the way, with no more card that shrinks and vanishes.
- **The lyrics button moved** into the player's three-dot menu ("Show lyrics" / "Show cover").
- **The mini player's progress bar** now takes a shade of the cover's color, never plain black, over a soft track.
- **The "Up next" handle** at the bottom of the player sits higher, away from Android's home gesture.

### 🐛 Fixes
- **Sharing a song** now sends a YouTube Music link instead of plain text.
- **Tapping the lyrics brings the cover back**, just like tapping the cover opened them. Hold a line to jump to it.
- **"No lyrics found" shows up quickly** when a song has no lyrics, instead of a misleading "Are you online?" message. That message now only appears when there's really no connection.

## [0.13.0] — 2026-10-17

### ✨ New
- **Artist biographies** in the About tab, from Last.fm, in your phone's language when available (English otherwise). Read the summary or tap **Read more** for the full text, see the artist's genre tags and how many listeners they have on Last.fm.

## [0.12.0] — 2026-10-17

### ✨ New
- **Album pages.** Tapping a release in an artist's discography now opens it inside the app: big cover, year and type, how many of its songs you have, and the full track list. Songs you have play right there; the ones you don't have a button to open them outside the app.
- **Three-dot menus** on songs and albums (library, player, artist, album): share the Deezer link (or the song's name), listen on YouTube Music, or open in Deezer.
- **Most played** in Settings → Wellbeing. A song counts after 30 seconds of real listening (skipping ahead doesn't add up), when you picked it yourself or it played in order. Songs that come up on their own in shuffle don't count, but picking one from the shuffled queue does. Everything stays on your phone.

### 🎨 Visual tweaks
- **The mini player takes a pastel tone from the current cover**, with a soft gradient, so it stands out from the navigation bar. Text switches between light and dark to stay readable.

### 🐛 Fixes
- **No more abrupt cuts:** opening and closing the Library search now animates, and with the lyrics open the back gesture closes just the lyrics (the cover slides back in) instead of the whole player.

## [0.11.0] — 2026-10-17

### ✨ New
- **Artist profiles.** Open one from a song's menu or by tapping the artist's name in the player.
  - A big header with the artist's photo fading into the background, their name, and a round picture on the edge.
  - How many of their songs and albums you have, and their fans on Deezer.
  - **Follow** (it will power the release radar), **Shuffle** and **Play** their songs that are on your device.
  - **Songs** tab: your songs by them, plus their most popular on Deezer, marking the ones you already have.
  - **Discography** tab: albums, EPs, singles and compilations, each one marked as in your library or not. Tap one to open it outside the app.
  - **About** tab: similar artists (tap to open their profile). The biography comes with Last.fm in a future update.
- The artist is matched by name, ignoring accents and "feat." guests; between artists with the same name, the most popular one is picked, and nothing is shown if no name matches.

## [0.10.0] — 2026-10-17

### ✨ New
- **Lyrics in the player.** Tap the cover (or the lyrics button at the top) and it smoothly turns into the song's lyrics; tap the button again to bring the cover back.
- **Synced lyrics**: the current line is big and bright, past and upcoming lines are smaller and faded, and the lyrics scroll along with the song. Tap any line to jump to that moment. If you scroll the lyrics yourself, they wait a few seconds before following the song again.
- Lyrics come from **LRCLIB**, a free and open lyrics database, and are saved on your phone after the first time. They're only looked up when you open them.
- **The player takes the color of the cover**: its background is tinted with the main color of the album art, changing smoothly from song to song.

## [0.9.0] — 2026-10-17

### ✨ New
- **A real shuffle.** Turning shuffle on now mixes the whole queue once, so **no song repeats until every song has played**, and the queue shows the true order. Turn shuffle off and the original order comes back, continuing from the song that's playing.
- **Reorder and remove work with shuffle on too.** Songs you remove while shuffling also leave the original order.
- **Shuffle button in the Library**: plays everything on screen (or just your search results) in random order.
- With **repeat all + shuffle**, every new round is shuffled again.
- Picking a song while shuffle is on plays it first and shuffles the rest.

## [0.8.0] — 2026-10-17

### ✨ New
- **Reorder the queue**: hold the handle on the right of any song and drag it anywhere in the queue, top to bottom.
- **Remove songs from the queue** with the button next to each one.

### 🐛 Fixes
- **While the queue is open, it owns the screen.** Swiping down anywhere, the arrow at the top and the back gesture now close only the queue; before, swiping above the queue closed the whole player along with it.

## [0.7.0] — 2026-10-17

### ✨ New
- **Playing queue**: the bottom of the player now shows what's up next. Tap it or swipe it up to open the full queue, in the real play order (shuffle included), with the current song highlighted. Tap any song to jump to it. Swipe the queue down, tap the arrow or use the back gesture to close it.

### 🎨 Visual tweaks
- **Shuffle and repeat look pressed when on**: they get a filled background instead of just changing color, so it's obvious at a glance.

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
