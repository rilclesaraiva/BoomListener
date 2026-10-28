# Changelog

All notable changes to **Boom, listener!** are recorded here, by version. This file is also the text of each GitHub Release, so it is written in English, for people who use the app.

Format inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/) — `MAJOR.MINOR.PATCH`.

## [0.52.4] — 2026-10-28

### 🎨 Visual tweaks
- The app is now built with Android's optimizer: a much smaller download, and screens and animations should feel smoother.

## [0.52.3] — 2026-10-28

### 🐛 Fixes
- The Privacy text in Settings now matches what the app really does: it mentions the release checks for artists you follow, the artist search, the update check when the app opens, offline mode and the notification permission, and no longer promises Last.fm scrobbling.

### 🎨 Visual tweaks
- Behind the scenes cleanup: leftover code from features that were removed is gone.

## [0.52.2] — 2026-10-28

### 🎨 Visual tweaks
- The oscilloscope visualizer is gone for now: it was heavy and too noisy with most music. Bars and Wave work as before; if you had picked the oscilloscope, Bars is used.

## [0.52.1] — 2026-10-28

### 🐛 Fixes
- Turning on the oscilloscope visualizer no longer closes the app.

## [0.52.0] — 2026-10-28

### ✨ New
- **Oscilloscope visualizer:** a third style in Settings → Appearance → Visualizer. The left channel moves the beam sideways and the right one up and down, sample by sample, like a real XY oscilloscope with a glowing, fading trace. Music made for oscilloscopes draws its pictures; any other song draws the shapes of its own stereo sound. The cover slowly fades into a dark screen while it's on and fades back when you turn it off.

### 🎨 Visual tweaks
- Every visualizer now fades in and out when you turn it on or off.

## [0.51.2] — 2026-10-28

> Everything since 0.45.1: Spanish, an audio visualizer over the cover, backup, background blur, a smoother player and many fixes. Still a beta: landscape and tablet screens may look rough in places.

### ✨ New
- **Spanish:** the whole app is now in English, Portuguese and Spanish. On Android 13 and newer you can pick the app's language on its own (Settings → Appearance → Language).
- **Audio visualizer over the cover:** bars (bass on the left, treble on the right) or a live wave of the sound, in the player's color. Turn it on in the player's ⋮ menu and pick the style in Settings → Appearance → Visualizer. It reads the sound from the player itself, so it needs no microphone permission.
- **Backup** (Settings → System → Backup): save your playlists, favorites, history, stats, profile, followed artists and settings to a file and bring them back, even on a new phone. Your songs aren't included, and the file only goes where you save it.
- **Background blur:** when a screen or the player opens on top, the one behind softly blurs (Android 12 and newer; Settings → Appearance → Effects).
- **Songs by several artists:** tapping the artist opens a panel with each artist, with their photo, so you land on the right page.
- **Artist page:** a bell next to "Following" turns release alerts off or on for just that artist, without unfollowing.
- **Playback options** (Settings → Playback): even out volume with ReplayGain, resume when headphones reconnect, remember shuffle, choose which lyrics come first, and turn gapless playback off if you want.
- **Stop when closed from recents** (Settings → System, off by default): swiping the app away stops the music and closes the app.
- **Fix tags automatically** finds more songs by several artists, is faster, doesn't refill tags that are already right, and lets you compare your cover with the one it found.
- **Profile color:** gradient or a solid color across the whole profile.
- **Update notices** (Settings → About): see whether they're paused or skipping a version, and turn them back on or pause them.

### 🐛 Fixes
- A Bluetooth headphone you renamed in Android's settings now shows that name.
- The fast-scroll bar no longer jumps, reaches the top in the Library and shows the right letter next to the thumb.
- Holding the player's cover no longer paints a color over it.
- Saving tags no longer waits for Android to re-read the file.
- Opening the app and Settings is lighter.
- The update notice shows up as soon as a new version is out, every time you open the app, and "Not now" only hides it until you open the app again.

### 🎨 Visual tweaks
- The player's progress dot glides smoothly with the song.
- Settings are reorganized: every option lives in a category, in a clearer order, with a new System page.
- Fade on play and pause is on by default.
- Playlist covers with 4 or more albums never repeat a cover; smaller playlists use the first song's cover.
- Sharing a song sends a YouTube Music search link (the service that found exact links shut down its free access).
- Portuguese: "Tirar" became "Remover" across the app.

## [0.51.1] — 2026-10-28

> Everything since 0.45.1: Spanish, an audio visualizer over the cover, backup, background blur, a smoother player and many fixes. Still a beta: landscape and tablet screens may look rough in places.

### ✨ New
- **Spanish:** the whole app is now in English, Portuguese and Spanish. On Android 13 and newer you can pick the app's language on its own (Settings → Appearance → Language).
- **Audio visualizer over the cover:** bars (bass on the left, treble on the right) or a live wave of the sound, in the player's color. Turn it on in the player's ⋮ menu and pick the style in Settings → Appearance → Visualizer. It reads the sound from the player itself, so it needs no microphone permission.
- **Backup** (Settings → System → Backup): save your playlists, favorites, history, stats, profile, followed artists and settings to a file and bring them back, even on a new phone. Your songs aren't included, and the file only goes where you save it.
- **Background blur:** when a screen or the player opens on top, the one behind softly blurs (Android 12 and newer; Settings → Appearance → Effects).
- **Songs by several artists:** tapping the artist opens a panel with each artist, with their photo, so you land on the right page.
- **Artist page:** a bell next to "Following" turns release alerts off or on for just that artist, without unfollowing.
- **Playback options** (Settings → Playback): even out volume with ReplayGain, resume when headphones reconnect, remember shuffle, choose which lyrics come first, and turn gapless playback off if you want.
- **Stop when closed from recents** (Settings → System, off by default): swiping the app away stops the music and closes the app.
- **Fix tags automatically** finds more songs by several artists, is faster, doesn't refill tags that are already right, and lets you compare your cover with the one it found.
- **Profile color:** gradient or a solid color across the whole profile.

### 🐛 Fixes
- A Bluetooth headphone you renamed in Android's settings now shows that name.
- The fast-scroll bar no longer jumps, reaches the top in the Library and shows the right letter next to the thumb.
- Holding the player's cover no longer paints a color over it.
- Saving tags no longer waits for Android to re-read the file.
- Opening the app and Settings is lighter.

### 🎨 Visual tweaks
- The player's progress dot glides smoothly with the song.
- Settings are reorganized: every option lives in a category, in a clearer order, with a new System page.
- Fade on play and pause is on by default.
- Playlist covers with 4 or more albums never repeat a cover; smaller playlists use the first song's cover.
- Sharing a song sends a YouTube Music search link (the service that found exact links shut down its free access).
- Portuguese: "Tirar" became "Remover" across the app.

## [0.51.0] — 2026-10-28

### ✨ New
- Spanish: the whole app is now in English, Portuguese and Spanish. On Android 13 and newer you can pick the app's language on its own (Settings → Appearance → Language).
- Artist page: a bell next to "Following" turns release alerts off (or back on) for just that artist, without unfollowing. It turns on by itself when you follow someone. The bell on the Releases screen still turns all release alerts on or off.
- Playlist covers have new shapes: 2 albums are split down the middle, 3 albums form a "Y" (one on top, two on the sides) and 4 or more make the 2 × 2 grid.

### 🐛 Fixes
- Playlist covers no longer repeat an album cover to fill the grid.

### 🎨 Visual tweaks
- Settings: the General list now follows a clearer order (Appearance, Playback, Library, Sharing, Wellbeing, System), and Backup moved inside System.

## [0.50.0] — 2026-10-28

### ✨ New
- Settings → System: a new "Stop when closed from recents" option (off by default). With it on, swiping the app away from your recent apps stops the music and closes the app completely.
- Songs by several artists: the panel with each artist now shows their photo in a circle, and it also opens from the song menus on Home, Library, Profile and History, not only in the player.

### 🎨 Visual tweaks
- Settings: every option now lives inside a category. Offline mode moved to the new Settings → System page.

## [0.49.0] — 2026-10-28

### ✨ New
- Songs by several artists ("Artist A, Artist B", "feat."): tapping the artist name in the player now opens a panel with each artist on their own, so you land on the right page. The full name is still there at the end, for bands with a comma or "&" in their name.

### 🎨 Visual tweaks
- The player's progress dot now glides smoothly along with the song, instead of hopping every half second.
- Visualizer: on covers with colors close to the visualizer's, the shade behind it gets darker so the bars and the wave stay easy to see.
- Settings → Appearance → Visualizer: each style now has a small drawing of how it looks.

## [0.48.0] — 2026-10-28

### ✨ New
- Audio visualizer over the cover: bars (bass on the left, treble on the right) or a live wave of the sound, in the player's accent color. Turn it on or off in the player's ⋮ menu, and pick the style in Settings → Appearance → Visualizer. It reads the sound straight from the player, so it needs no microphone permission.
- Fix tags automatically: when your tags already match but the song has a cover, you now see your cover next to the one it found and can tap "Use this cover" to swap just the cover.

### 🐛 Fixes
- Opening the app is lighter: the headphone name is looked up once after the system settles, instead of on every audio route update.

## [0.47.1] — 2026-10-27

### 🐛 Fixes
- Opening Settings or the tag editor with background blur on no longer stutters.
- Fix tags automatically: when your tags already match what it finds (and the song already has a cover), it now says so and changes nothing, instead of filling in the same data again.

## [0.47.0] — 2026-10-27

### ✨ New
- Background blur: when a screen or the player opens on top, the screen behind it softly blurs. On by default; turn it off in Settings → Appearance → Effects (Android 12 and newer).
- Gapless playback can now be turned off in Settings → Playback (it stays on by default).

### 🐛 Fixes
- A Bluetooth headphone you renamed in Android's settings now shows that name in the player, not the factory name.
- Fix tags automatically finds more songs by several artists ("Artist A, Artist B y Artist C", "feat.", "x", "&"): it also searches each artist on their own, and hashtags in the title ("#JerseyClub") no longer get in the way. The artist field is filled with every artist of the song, with guests after "feat.".
- Tag editor: fixing tags automatically is faster (searches run at the same time), and saving no longer waits for Android to finish re-reading the file.
- Player: holding the cover no longer paints a color over it.

### 🎨 Visual tweaks
- Fade on play and pause is now on by default. If you had turned it off, it stays off.

## [0.46.5] — 2026-10-25

### 🐛 Fixes
- Library: the fast-scroll bar no longer flashes for an instant up by the chips before settling below them.

## [0.46.4] — 2026-10-25

### 🐛 Fixes
- Library → Artists: the fast-scroll bar starts below the search box again, instead of going up beside the chips.

## [0.46.3] — 2026-10-25

### 🐛 Fixes
- Fast-scroll bar: the letter bubble now shows the letter of the song, artist or genre right next to the thumb on screen.
- Library → Genres and Artists: dragging the bar all the way up now takes you back to the top of the page.

## [0.46.2] — 2026-10-25

### 🐛 Fixes
- Fast-scroll bar: the letter bubble now shows the letter of the item next to the thumb (on Home and in Library → Genres and Artists). Before, it could get stuck on the first letter.
- Profile with a solid color: the whole page now is that exact color (#000000 is true black, in any theme), and the text and cards take matching shades so everything stays readable.

### 🎨 Visual tweaks
- Sharing a song now always sends a YouTube Music search link. The service that turned Deezer and Apple links into exact YouTube Music links (song.link) shut down its free access, so the exact link wasn't working anyway.

## [0.46.1] — 2026-10-25

### 🐛 Fixes
- Fast-scroll bar: it no longer jumps around when you scroll slowly. It now follows the real position of the list, and in the Library its track stays put instead of stretching as the page header scrolls away.

### 🎨 Visual tweaks
- Sharing a song finds the exact YouTube Music link more often: when Deezer doesn't have the song, the app now also looks it up in Apple's music catalog. If neither is sure it's the same song, you still get the search link, never the wrong song.

## [0.46.0] — 2026-10-25

### ✨ New
- **Backup** (Settings → Backup): save your playlists, favorites, history, stats, profile, followed artists and settings to a file, and bring them back later, even on a new phone. Songs are found again by file name, so your playlists keep working. Your songs themselves aren't included, and the file only goes where you save it.
- **Even out volume (ReplayGain)** (Settings → Playback): songs that carry ReplayGain tags play at a similar volume. Loud songs get quieter; nothing is boosted, so nothing distorts.
- **Resume when headphones reconnect** (Settings → Playback): if the music paused because your headphones disconnected, it plays again when they come back (within 30 minutes).
- **Remember shuffle** (Settings → Playback): when you open the app again, shuffle stays on if it was on.
- **Lyrics source** (Settings → Playback → Lyrics): choose whether the lyrics inside the file or the online ones (LRCLIB) come first.
- **Profile color:** choose between the gradient (as before) and a solid color across the whole profile.

### 🐛 Fixes
- Library → Genres and Artists: the fast-scroll bar now shows up on short lists too, runs from the first item to the last, and never goes over the page header. The letter bubble only shows in list view.

## [0.45.1] — 2026-10-24

**Fourth public beta.** 🎧 This release brings together everything since 0.42.4: a share banner, an offline mode, a few handy new options and a lot of fixes to the search boxes. Here are the highlights; the full list is below in this file.

> 🧪 **Still a beta.** The phone in portrait is the main target. Landscape and tablet layouts still have broken spots we're working on. Please report anything odd in Issues.

### ✨ New since 0.42.4
- **Share banner** (player menu → "Share banner"): a story-sized card with the cover, the song and the artist, for Instagram stories, WhatsApp status and the like. Pick one of three color styles and share just the image or a video with 30 seconds of the song. Only a 30-second clip ever goes out, never the whole song.
  - Choose the 30 seconds on a bar that shows the whole song, with the clip as a window you drag. While the panel is open, only that clip plays, on repeat; the song you were listening to picks up again when you close it.
  - The bars on the banner come from the sound of the clip itself: in the image they draw the 30 seconds from start to end; in the video they move like an equalizer, bass on the left and treble on the right.
- **Offline mode** (Settings): the app doesn't go to the internet at all (no artist pages, photos, lyrics or release radar); what you've already seen keeps showing. Only the app-update check still works. Off by default.
- **Add to queue:** hold a song (or tap ⋮) and choose "Add to queue" to play it right after the current one.
- **Playlist covers** made of four album covers when a playlist has songs from different albums.
- **New playlist and album pages,** with a big cover across the top and a big round Play button.

### 🐛 Fixes since 0.42.4
- Search boxes (Home and Library → Artists): typing no longer makes the page jump, shake or flicker. The box sticks right under a solid status bar while you type, holding backspace deletes letter after letter again, and there's no huge empty space after the last result.
- Library → Artists: the fast-scroll bar is always there for your artists and starts below the search box; the letter bubble only shows in list view, not in grid view or while searching.

## [0.45.0] — 2026-10-24

### ✨ New
- Share banner: the little bars now come from the sound of the chosen 30 seconds. In the image they draw the clip from start to end; in the video they pulse along with the music.
- Share banner: a new clip bar. The whole song is the bar and the 30 seconds are a highlighted window with a clear start and end; drag it (or tap where you want it) to move it. A marker inside the window follows the clip as it plays.

### 🐛 Fixes
- Share banner: the clip preview no longer keeps playing when you leave the app; it picks up again when you come back.

## [0.44.1] — 2026-10-24

### 🎨 Visual tweaks
- Share banner: the clip now starts at the beginning of the song. While the panel is open, the song you were playing pauses and only the chosen 30 seconds play, on repeat; moving the bar changes the clip, not the song you were listening to, which picks up again when you close the panel.
- Library → Artists: the fast-scroll bar is always there for your artists, starting right below the search box. The letter bubble only shows for your artists in list view (they're in alphabetical order), not in grid view or while searching.

## [0.44.0] — 2026-10-24

### ✨ New
- Share banner: in the player menu, "Share banner" makes a story-sized card with the cover, the song and the artist, ready for Instagram stories, WhatsApp status and the like. Pick one of three color styles, then share just the image or a video with 30 seconds of the song. Drag the bar to choose which 30 seconds; the song jumps there so you can hear it. Only a 30-second clip ever goes out, never the whole song.

### 🐛 Fixes
- Search results (Home and Library → Artists) no longer leave a huge empty space after the last result.
- Library → Artists: the fast-scroll bar stays below the search box instead of going over the top of the page.

## [0.43.6] — 2026-10-24

### 🐛 Fixes
- Search boxes (Home and Library → Artists): while you type near the top, the box now sticks right under a solid status bar and stays there. Before, each letter nudged the page back a few pixels, so the box came loose, the status bar flickered and the page seemed to tremble.

## [0.43.5] — 2026-10-24

### 🐛 Fixes
- Search boxes: the box now stays exactly at the top for every letter you type, not only the first, so no cut-off bits show above it.
- The status bar above the search boxes only turns solid once the box is stuck to the top. Before, it turned solid a bit early and cut off what was passing under it.

## [0.43.4] — 2026-10-24

### 🐛 Fixes
- Search boxes (Home and Library → Artists): on the first letter the box moves up to sit right under the status bar, so no cut-off bits of what's above it show at the top. After that the page stays still while you type.

## [0.43.2] — 2026-10-24

### 🐛 Fixes
- Typing in the search boxes (Home and Library → Artists) no longer makes the page shake, and holding backspace deletes letter after letter again.

## [0.43.1] — 2026-10-23

### 🐛 Fixes
- Typing in a search box no longer makes the page jump a little with each letter.

### 🎨 Visual tweaks
- Playlist covers: the four-cover mosaic now shows up whenever a playlist with more than 3 songs has songs from at least 2 different albums (covers repeat diagonally when there are fewer than 4 albums). Only playlists where every song is from the same album keep a single cover.

## [0.43.0] — 2026-10-23

### ✨ New
- **Offline mode** (Settings): when it's on, the app doesn't go to the internet at all: no artist pages, photos, lyrics or release radar. Only the app-update check still works. Photos and covers you've already seen keep showing. It's off by default.
- **Add to queue:** hold a song (or tap ⋮) and choose "Add to queue" to make it play right after the current one.
- **Playlist covers:** playlists with more than 3 songs from at least 4 different albums get a cover made of four album covers.
- **New playlist and album pages:** a big cover across the top (the four-cover mosaic for playlists), the name centered over it, and a big round Play button with Shuffle next to it. It's on purpose different from artist pages, so you can tell them apart at a glance.

### 🎨 Visual tweaks
- The search boxes on Home and in Library → Artists sit closer to what's above them, and the songs below them come up with them.
- The "More artists" line in the artist search keeps the same height while it loads, so the list no longer jumps a little as you type.

## [0.42.4] — 2026-10-23

**Third public beta.** 🎧 This release brings together everything since 0.40.3: a redesigned player and a lot of polish. Here are the highlights; the full list is in CHANGELOG.md.

> 🧪 **Still a beta.** The phone in portrait is the main target. Landscape and tablet layouts still have broken spots we're working on. Please report anything odd in Issues.

### ✨ New since 0.40.3
- **The mini player grows into the full player.** Pull it up and it stretches to fill the screen, pushing the tab bar down, while its info fades into the full player. Pull down and it shrinks back.
- **New player controls** (Material 3 Expressive): previous, play and next fill the whole width as one group. Play is the biggest button, round while paused and squarer while playing, and the buttons squish a little when pressed. Shuffle and Repeat are wide buttons with their names.
- At the bottom of the player: where the sound is playing (with the device name) and what's up next.
- Playing a song from a list puts it at the top of the queue, with the rest of the list after it.

### 🐛 Fixes since 0.40.3
- With the queue open, the player behind it is dimmed and can't be tapped by accident; the queue stops at its last song and no longer shows an empty space.
- Shuffle no longer makes the covers flash or race past on the player.
- Messages at the bottom of the screen show above the mini player instead of behind it.
- Library → Artists: the search box sticks just below the status bar while you type, like the one on Home.

### 🎨 Visual tweaks since 0.40.3
- The mini player floats over the page, in the same color as the full player, with a faint glow around it.
- Artist page: no top bar, the artist's initial while the photo loads, and the round picture is always the newest release's cover.
- Home: the title scrolls with the page, the status bar turns solid under the sticky "Songs" header, and the fast-scroll bar starts below that header.

## [0.42.3] — 2026-10-23

### 🐛 Fixes
- Messages at the bottom of the screen (like the one from the bell on Releases) now show above the mini player instead of behind it.

### 🎨 Visual tweaks
- Home: the fast-scroll bar starts below the "Songs" header instead of running up beside the search box.

## [0.42.2] — 2026-10-23

### 🎨 Visual tweaks
- The mini player now has a faint glow around it, in the cover's color, so it stands out from the page behind it.

## [0.42.1] — 2026-10-23

### 🐛 Fixes
- Turning on Shuffle no longer makes the covers race past on the full player. The cover of the current song stays still.

### 🎨 Visual tweaks
- The mini player now has the same color as the full player's background (a tint from the cover), and the card that grows between them uses that color too, so the change from one to the other looks like the same thing growing.

## [0.42.0] — 2026-10-23

### ✨ New
- **The mini player grows into the full player.** Pull it up and it stretches to fill the screen, pushing the tab bar down out of the way, while the mini player's info fades out and the full player fades in. Pulling the full player down shrinks it back into the mini player, and the tab bar comes back.

### 🐛 Fixes
- Turning on Shuffle no longer flashes the song's initial on the cover for a moment.

## [0.41.0] — 2026-10-23

### 🎨 Visual tweaks
- **New player controls**, following Material 3 Expressive: previous, play and next now fill the whole width as one group. Play is the biggest button and the only one in the main color. It's round while paused and turns squarer while playing, and every button squishes and stretches a little when you press it.
- Shuffle and Repeat are now wide buttons with their names, filled with color when they're on.
- The bottom of the player shows two small buttons: where the sound is playing (with the device name, like "Bluetooth headphones") and what's up next. The sound output moved there from the top of the player.

## [0.40.5] — 2026-10-23

### 🐛 Fixes
- The queue no longer opens an empty space below the last song when you scroll it while music is playing.
- Playing a song from a list now puts it at the top of the queue, with the rest of the list after it. Before, picking the last song of a list left the queue at its very end.

### 🎨 Visual tweaks
- Home: the status bar is see-through while you scroll the top of the page, and turns solid in the color of the "Songs" header once that header sticks to the top.

## [0.40.4] — 2026-10-23

### 🐛 Fixes
- With the queue open, the player behind it is dimmed and can no longer be tapped by accident. Tapping the dimmed part closes the queue.
- The queue stops at its last song: pulling hard no longer shows an empty space below it.

### 🎨 Visual tweaks
- The mini player now floats over the page, with no strip behind it (you could see that strip when closing the mini player).
- Artist page: while the photo loads, the big area shows the artist's initial instead of a cover from your phone, and the small round picture is always the cover of their newest release.
- Artist page: no more top bar when you scroll. Only the back button stays in the corner.
- Home: the title is now part of the page and scrolls with it, like in Library, Profile and Settings.

## [0.40.3] — 2026-10-22

**Second public beta.** 🎧 This release brings together everything since 0.36.2. Here are the highlights; the full list is in CHANGELOG.md.

> 🧪 **Still a beta.** The phone in portrait is the main target. Landscape and tablet layouts still have broken spots we're working on. Please report anything odd in Issues.

### ✨ New since 0.36.2
- **Release alerts:** your phone tells you when an artist you follow releases a new album, EP or single. Turn them on or off with the bell on the Releases screen.
- **New menus:** hold any song, album, artist, genre or playlist (and the songs and artists on your profile) to open a menu with the cover on top and icons for each option.
- **Mix:** from the player menu, keep the current song playing and fill the queue with similar songs first, then the rest of your library.
- **Grid or list in Library**, remembered for each tab.
- **Smarter progress bar:** the dot snaps back to where the song is playing (an easy way to give up on a seek), and the time on the left keeps showing the real position.
- **Better automatic tags:** they now also search the artist's own songs and albums on Deezer, so remixes and EP tracks are found.
- **Go to album** in song menus, and **Open queue** in the player menu.

### 🐛 Fixes since 0.36.2
- Shuffle no longer goes back to the song that was already playing, and swiping between songs no longer flashes the previous one.
- Going to an artist from the full-screen player and coming back reopens the player.
- Seeking works in MP3 and AAC files without an index, and songs in formats your phone can't play are skipped with a message instead of stopping playback.
- The back gesture on panels uses the app's own animation, and panels close only when you mean to (Edit profile closes only from the strip at its top).
- Animated profile pictures from videos or big GIFs play much smoother.
- The "new version available" screen now shows up on its own when you open or come back to the app, always with the notes of the newest version. Before, it could stay hidden for hours.

### 🎨 Visual tweaks since 0.36.2
- The top bar hides as you scroll down and comes back when you scroll up. On your profile, in Library and in Settings, the title is part of the page.
- Back buttons are an arrow in a circle everywhere. Panels no longer cover the whole screen.

## [0.40.2] — 2026-10-22

### 🐛 Fixes
- Edit profile only closes when you pull from the strip at its top (where the handle is). Scrolling the form and pulling past the top no longer closes it by accident.

## [0.40.1] — 2026-10-22

### 🐛 Fixes
- Seeking in MP3 and AAC files without an index now lands where you drag, instead of the wrong spot or nowhere.
- A song in a format your phone can't play (like WMA, AIFF, DSD or, on most phones, ALAC) no longer stops everything: the app tells you and skips to the next song.
- Edit profile closes by swiping it down, like the other panels, and the X button is gone.

### 🎨 Visual tweaks
- Panels that slide up no longer reach the top of the screen, so it's clear you can pull them down.
- Progress bar: while you drag the dot, there's now a small gap between the played part and the rest, like the one around the dot.
- Back buttons are now an arrow inside a circle on every screen. In Settings it stays fixed in the corner while the page scrolls, and titles start further right so the button never covers them.
- About now says the app is made in Brazil.

## [0.40.0] — 2026-10-22

### ✨ New
- **Release alerts:** your phone now tells you when an artist you follow releases a new album, EP or single. The app checks about twice a day, and tapping the alert opens Releases. Turn alerts on or off with the bell on the Releases screen. On Android 13 and newer, the app asks for permission to show notifications.
- **Grid or list in Library:** a new button next to the tabs switches genres, artists, playlists and albums between a grid of big covers and a list. The app remembers your choice for each tab.

### 🎨 Visual tweaks
- Library and Settings no longer have a bar pinned to the top of the screen. The title is part of the page and scrolls away with it, like on your profile.

## [0.39.2] — 2026-10-22

### 🐛 Fixes
- The Profile tab scrolls again. Since the last version, you could tap things but not move the page.

## [0.39.1] — 2026-10-22

### 🐛 Fixes
- Automatic tag fixing now also looks inside the artist's albums on Deezer, the same way the artist page marks what you have. A song like "SKY.exe (Slowed)" is found in its EP instead of suggesting other songs of the artist.
- Suggestions that only share a loose word with your song (like "Slowed") no longer show up as options.

### 🎨 Visual tweaks
- Profile: the "Your profile" title is now part of the page. It sits at the top and scrolls away with everything else, instead of staying pinned on the screen.

## [0.39.0] — 2026-10-22

### ✨ New
- **Hold for the menu everywhere:** genres, artists, playlists and Favorites in Library, artists on Home, and on your profile the most played songs, your artists and the history row now open their menu when you hold them. Groups can be played, shuffled or added to a playlist; songs can be played, favorited, added to a playlist, edited or opened by artist.

### 🎨 Visual tweaks
- Your profile's top bar is transparent again and stays fixed at the top: it no longer hides while scrolling, and the page no longer shows behind the title.

## [0.38.0] — 2026-10-22

### ✨ New
- **Album menu:** hold an album (on Home or in Library → Albums) to play it, shuffle it, add it to a playlist, go to the artist or open it in YouTube Music.

### 🐛 Fixes
- The back gesture on a menu or panel no longer shrinks it with the system's animation. It now closes by sliding down, the app's own way.
- Going to an artist (or to the tag editor) from the full-screen player and then going back now reopens the player, as it was.
- Progress bar: the line stays straight while you drag the dot and waves again when you let go. The time on the left keeps showing where the song really is (the bubble shows where you're going). Dragging the dot no longer makes the player jump as if it were closing.
- Home: with the top bar hidden, the "Songs" header no longer slides under the status bar.
- Automatic tag fixing now also looks at the artist's songs on Deezer, the same place the artist page uses. Songs that show up on the artist page (like remixes where Deezer lists someone else as the main artist) are no longer "not found".

### 🎨 Visual tweaks
- When the top bar hides, the status bar area becomes transparent instead of keeping a colored strip.
- Your profile's top bar is solid again, in your profile color, so the page no longer shows through it.

## [0.37.0] — 2026-10-22

### ✨ New
- **New song menu.** The ⋮ menu (or holding a song) now opens a panel from the bottom, with the cover and name of what you picked at the top and an icon next to each option. It only shows what makes sense for that item: a song, an album, an artist's release or a playlist.
- **Go to album** in the song menus.
- **Mix** in the player menu: keeps the current song playing and fills the queue with the rest of your library, mixed. The first few are similar songs (same artist or genre). Great when the queue has just one song.
- **Open queue** in the player menu.
- **Progress bar:** while you drag the dot, the bar keeps showing where the song really is, and the dot shows where you're going. Near the current spot, the dot snaps to it (with a small vibration), so letting go there changes nothing. The easy way to give up on a seek.
- **Hiding top bar:** scrolling down hides the top bar to give the list more room. Scrolling up brings it back, and it's always there at the top of the page.

### 🐛 Fixes
- Tapping Shuffle again sometimes went back to the song that was already playing. Each tap now starts a fresh shuffle with a different song.
- Swiping the mini player or the cover no longer flashes back to the previous song for an instant.
- Typing in a search (Home songs, Library artists) brings the screen back to the search bar, even if you had scrolled away.
- Animated profile pictures and covers made from videos or big GIFs play much smoother (up to 15 frames per second instead of 5). Pick the GIF or video again to get the smoother version.

### 🎨 Visual tweaks
- Your profile's color now tints the top bar too, so the whole page has the same color.

## [0.36.2] — 2026-10-21

**First public beta of Boom, listener!** 🎧 A local music player for Android: free, open source, no ads, no account, and your data never leaves your phone.

> 🧪 **This is a beta.** Day-to-day use on a phone in portrait works well, and that's where most of the polish went. **We know some layouts are still broken in other modes, mainly landscape and tablets.** We're working on them, and the next versions will fix them. If you find a problem, please report it in Issues.

### ✨ What's in it
- **Player:** mini player and full-screen player with a wavy progress bar, queue you can reorder, shuffle and repeat, synced lyrics, sleep timer, speed and crossfade. Swipe the cover or the mini player to change songs.
- **Your library:** Home with recently added songs, albums and artists, plus Library with genres, artists, playlists and albums. Search, fast scroll and Favorites.
- **Tag editor:** fix titles, artists, albums, years, genres, covers and lyrics, with suggestions from Deezer and MusicBrainz.
- **Artist pages:** photo, biography, popular songs and the full discography, showing what you have.
- **Releases:** follow artists and see their new albums, EPs and singles.
- **Your profile:** name, photo, cover and color, just for you, with your listening stats and history.
- **Wide screens (beta):** a side bar, plus a panel with the artist that's playing.
- Light, dark and AMOLED themes, wallpaper colors, English and Portuguese.

### 📝 Also in this version
- The README now explains how the app is made: with AI assistance, under the author's direction, testing and review.

## [0.36.1] — 2026-10-21

### 🐛 Fixes
- Dragging the progress dot in the player shows the time you're on, in a bubble above the dot and in the time on the left.
- Wide screens: opening an artist's page hides the artist panel and shrinks the side bar, so the page gets the whole space. Tapping the same artist again no longer opens a second copy.
- Wide screens: the whole side bar scrolls now, not just the list of artists you follow.
- Wide screens: the shrunk side bar is wider and keeps all its buttons, including Home and Library.

### 🎨 Visual tweaks
- Wide screens: the top bar keeps its color when you scroll. Before, only the middle strip changed color.

## [0.36.0] — 2026-10-21

### ✨ New
- **A new layout for tablets and landscape.** With a wide screen, the bottom bar becomes a side bar with Releases, Home, Library, New playlist, Favorites and the artists you follow. The side bar can shrink to just icons.
- **Artist panel while a song plays.** On a wide screen, the right side shows the artist that's playing: photo, Follow, biography and popular songs. Hide it with the arrow, and bring it back the same way.
- Your profile photo and Settings sit at the top right on wide screens.
- **Favorites.** Tap the heart in the player, or "Add to favorites" in any song menu. Your favorites are the first item under Library → Playlists.
- **Player in landscape:** the cover goes on the left, and the song info and controls on the right.

## [0.35.2] — 2026-10-21

### 🐛 Fixes
- Right after going back, tapping an album, artist or anything else opens it on the first tap. Before, the tap showed but nothing opened until you tried again.

## [0.35.1] — 2026-10-21

### 🐛 Fixes
- Going back from an album, artist or any other screen now always slides the right way. Sometimes the screen slid off the wrong way and stayed faded on top of the previous one.
- Swiping between songs in the full player no longer flashes the previous cover for a moment.

## [0.35.0] — 2026-10-20

### ✨ New
- **Swiping to change songs feels instant.** While you drag the cover, in the player or on the mini player, the next or previous song's cover already shows next to it. Let go and the song changes right away, with no empty screen in between.

### 🐛 Fixes
- Going back no longer leaves two screens on top of each other, half transparent. If a screen is still sliding in or out, a second "back" waits for it to finish.
- On Home, the scroll bar now only covers the Songs section. It no longer goes up over Recently added, Albums and Artists.

### 🎨 Visual tweaks
- The Up next list no longer shows a letter bubble when you drag its scroll bar, since it isn't in alphabetical order.

## [0.34.0] — 2026-10-20

### ✨ New
- **Releases remember your choices.** Filter (albums, EPs, singles), view and time range stay as you left them.

### 🐛 Fixes
- The letter in the scroll bar bubble now follows the handle: at the bottom of the bar it shows the end of the list, not a song from the top of the screen.
- Tapping fast no longer opens the same album, artist or screen twice, one on top of the other. Tapping back twice quickly no longer closes two screens.

### 🎨 Visual tweaks
- Cleanup: removed leftover pieces of old features that no longer did anything. The app is a little smaller.

## [0.33.0] — 2026-10-20

### ✨ New
- **Your profile is just for you now.** Name, description, color, photo and cover make the app feel like yours. Everything stays on your phone and nothing is sent anywhere. Your current name, description, color, photo and cover carry over.
- Tap your photo, your cover or your name to edit your profile.

### 🐛 Fixes
- The scroll bar on the right now matches the list. The letter in the bubble is the song at the top of the screen, and dragging to the bottom reaches the last song.

### 🎨 Visual tweaks
- Friends and everything social are gone: friends, what friends are listening to, recommendations, reactions, sharing what you listen to and social networks on the profile. "Friends and sharing" is gone from Settings too.

## [0.32.0] — 2026-10-20

### ✨ New
- **Swipe to change songs.** On the mini player or on the cover in the player, swipe left for the next song and right for the previous one.
- **Find artists you don't have yet.** Library → Artists now has a search field. It shows your own artists and, below them, more artists from the internet. Tap one to open their page.
- **Friends see your song's cover.** When you share what you're listening to, your friends now see the album cover too.
- **Social networks moved into "Edit profile".** Your networks show up as buttons in a row you can swipe. Tap "Add network", pick the network and type your @ or number.

### 🐛 Fixes
- Tapping Home to go back to the top now scrolls all the way up in one smooth move, with no stops along the way.
- Typing in the Home search no longer makes the screen jump.
- The scroll bar on the right of long lists now moves smoothly instead of jumping song by song.
- Going back from the cover to the lyrics keeps your place in the lyrics.
- In Devices, the device you pick is the one that gets marked. Switching to headphones no longer left the phone marked.
- "Edit profile" no longer closes when you swipe it down by accident. Close it with the X, or by tapping outside it.

### 🎨 Visual tweaks
- What your friends are listening to now shows one card per friend, in a row you can swipe.
- The bell for Releases is hidden on your profile.
- Menu options are in alphabetical order.
- In Portuguese, the playlist menu now says "Remover da playlist".

## [0.31.0] — 2026-10-20

### ✨ New
- **New tab: Profile.** It replaces Friends. Your stats, history, social networks and friends now live together. Friend requests, what your friends are listening to and the songs they recommend are all on your profile.
- **Releases moved to the top bar:** tap the bell next to Settings, on any tab.

### 🎨 Visual tweaks
- The bottom bar is simpler: Home, Library and Profile.
- The history button left the top bar. Your full history is on your profile, under "History".
- The top bar now shows Releases first, then Settings.

## [0.30.0] — 2026-10-20

### ✨ New
- **Friends is now "What your friends are listening to".** Chat is gone: you talk where you already are. Each friend gets a card with the song they're playing now, or the last one they heard and when.
- **Friend requests.** Find someone by their @ and add them. They become your friend when they accept. Requests show up at the top of Friends.
- **Your social networks on your profile:** Instagram, X, TikTok, Threads, Bluesky, YouTube, Last.fm, SoundCloud, Bandcamp, Spotify and Telegram. You only type your @. The app builds the link, so nobody can sneak in a fake page.
- **WhatsApp and Telegram by number, for friends only.** It's off by default. Turn on "Show my number to my friends" and only your friends see it. The country code comes from your phone and you can change it.
- **React with an emoji** (🔥 ❤️ 😂 😮 👏 🎧) to what a friend is listening to right now. They see it pop up for a moment.
- **Recommend a song with one tap:** pick friends from the song menu. They get a card with the cover, artist and link under "Recommended to you". If they have the song, tapping it plays it.

### 🐛 Fixes
- Conversations, groups, mailbox and message notifications are gone, and so is the notification permission. Old conversations are erased from your phone.

## [0.29.0] — 2026-10-20

### ✨ New
- **Messages with the app closed**, with no Google involved. Turn it on in Friends: the free **ntfy** app delivers a tiny "wake up" signal (never your messages), and you get a notification when someone writes to you. If you don't have ntfy, the app offers to install it.
- **Message status:** your messages now show **Waiting** (it stays on your phone until the other person comes online), **Delivered**, or **In their mailbox** (kept on the server for up to 7 days).
- Your conversations are no longer copied to Android's cloud backup. They live only on your phone.

### 🎨 Visual tweaks
- Profile photos and covers are sharper (512 px and 1080 px) and smaller to download (WebP).
- Animated GIFs that are too big, too large or longer than 15 seconds are shrunk automatically. Short videos can be up to 15 seconds.
- Friends' profiles load faster: the app only downloads a profile again when it has changed.

## [0.28.0] — 2026-10-20

### ✨ New
- **Friends is live.** While the app is open, it stays connected to the server instead of asking for news every few seconds. Messages and what your friends are listening to show up right away.
- **"Listening now" keeps working in the background** while music plays, if you share what you listen to.
- If the free server runs out of its daily limit, Friends shows **"resting until…"** with the time it comes back. Music is never affected.

### 🐛 Fixes
- Sending the same message twice after a bad connection no longer creates a duplicate.
- After losing the connection, the app waits a little longer between retries, so it doesn't drain your battery.

## [0.27.0] — 2026-10-20

### ✨ New
- **MusicBrainz as a backup source.** When Deezer doesn't know a song or an artist, the app now asks MusicBrainz, an open music database:
  - **Fix tags automatically** finds the title, artist, album, year and track number there, with the cover from the Cover Art Archive.
  - **Artist pages** show the discography (albums, EPs, singles and compilations, with covers) even for artists Deezer doesn't have. If there are several artists with the same name, the app picks the one whose albums you have.

## [0.26.0] — 2026-10-20

### ✨ New
- **Playback settings** (Settings → Playback):
  - **Crossfade:** the song that's ending fades out while the next one fades in. Pick how many seconds with a slider, or turn it off.
  - **Fade on play and pause**, so the sound never cuts off abruptly.
  - **Playback speed**, from 0.5× to 2×, without changing the pitch. Also in the player's ⋮ menu.
  - Gapless playback is always on: albums that flow from one track into the next play without a pause.
- **Sleep timer** in the player's ⋮ menu: 15, 30, 45, 60 or 90 minutes, or at the end of the current song. The sound fades out gently before it stops.
- **Favorite share app** (Settings → Sharing): pick one of your apps, or type its package name. Song menus then show **Share with** that app, one tap away.
- **Profile color:** choose one from the palette or type any hex code. Your profile, and the way friends see it, takes on that color.
- **Big GIFs and short videos as your profile photo or cover.** A GIF over the limit is shrunk to fit, and a video of up to 30 seconds becomes a looping GIF (without sound).
- **Details** in every song's ⋮ menu: where the file is, format, size, length, bitrate and sample rate.

### 🐛 Fixes
- Jumping back to the top of a long list (tapping the tab again) is now smooth all the way up.
- Holding the drag handle in the queue or in a playlist no longer opens the song menu.
- The search bar on Home stays in place while you type, instead of the list jumping up and down. Tap ✕ to clear it.

### 🎨 Visual tweaks
- Albums and artists on Home keep the same layout even when a name takes two lines.

## [0.25.0] — 2026-10-19

### ✨ New
- **Friends on your profile.** The bottom section now shows the people you talk to, with their photos, instead of the artists you follow (those are already in Releases).
- **Hold a song anywhere** to open its menu. It now also works in Recently added, History, artist pages and Deezer albums.
- Long titles in **Recently added** now glide to the end, pause for a moment and glide back, instead of being cut off.

### 🐛 Fixes
- **Animated GIFs now keep playing** as your profile photo or cover. Bigger GIFs are accepted too (up to 5 MB).
- **Edit profile** only changes your profile when you tap **Save**. Until then, the new photo or cover is just a preview.
- After jumping back to the top of a list (by tapping the tab again), the top bar no longer stays in its grey "scrolled" color.

### 🎨 Visual tweaks
- The song menu no longer shows the song length.

## [0.24.0] — 2026-10-19

### ✨ New
- **Search right on Home.** The search bar now sits next to the "Songs" title instead of in the top bar. Type and the list filters as you go.
- **Hold a song** to open its ⋮ menu. This works on Home, in albums and playlists, and in the queue.
- **Delete from device**, in every song's ⋮ menu. Android asks you to confirm, and the song also leaves the queue.
- **Playlists:** add or remove songs as many times as you like. "Add to playlist" now shows a ✓ on the playlists that already have the song, and tapping again removes it. Inside a playlist, **Add songs** (a button and in the ⋮ menu) lists all your songs with search and checkboxes.
- **Animated GIFs** as your profile photo or cover (up to 1.4 MB).
- Artists on your profile now show their pictures: the Deezer photo for artists you follow, or a cover from your songs.

### 🐛 Fixes
- Switching quickly between output devices no longer leaves the sound stuck on the wrong one.
- The song that's playing keeps its ⋮ menu in the queue.

### 🎨 Visual tweaks
- The player top bar is cleaner: "9 of 72" moved into the queue header.
- The shuffle button for recently added songs is gone.
- The dot at the end of the progress bar fades out as the song reaches the end.
- The fast-scroll handle uses the theme color (no more grey bar in AMOLED).

## [0.23.0] — 2026-10-18

### ✨ New
- **Your profile, your way.** You can now add a **cover banner**, a **profile photo** and a short **description** (up to 160 characters). To change them, tap the pencil, or tap your photo. Photos are picked with Android's photo picker, so the app needs no extra permission, and they're resized before upload.
- Your friends see your cover, photo and description on your profile. Photos also appear in the Friends tab and in the top-right shortcut.
- **"See all"** on Home: the Albums and Artists sections now have a button that opens the Library tab right on that list.

### 🎨 Visual tweaks
- On Home, the song count and the shuffle button for recently added songs sit further from the edge, so they no longer collide with the fast-scroll handle.

## [0.22.0] — 2026-10-18

### ✨ New
- **A new Home.** Shuffle and Play buttons sit at the top. Below them are **Recently added** (numbered, with its own shuffle button), your **Albums** and your **Artists**, followed by the full song list.
- **Update notice.** When a new version comes out, the app shows what changed in it, with buttons to download it now or later.
  - The muted bell pauses these notices for a week or a month, or skips that version entirely.
  - The app checks for updates at most every 6 hours.
- **A new About page**, with the app icon, version and build type, the developer, the source code, what's new and a place to report problems.

### 🎨 Visual tweaks
- The wave on the player's progress bar now runs right into the round handle, with no gap in between.

## [0.21.0] — 2026-10-18

### ✨ New
- **Connected devices.** A new button at the top of the player shows where the sound is coming out (this phone, Bluetooth, wired headphones…). Tap it to see every connected device and send the music to another one. "More devices" opens Android's own output picker.
- **Fast scrolling.** Start scrolling a long list and a handle appears on the right. Drag it to jump through the list, with a bubble showing the letter you're at. It works on Home, the queue, albums, playlists, genres and the Library tab. In History, the bubble shows the day instead ("Today", "Yesterday"…).
- **Total length** of albums, playlists and genres, next to the song count (for example "1 h 05 min").

### 🎨 Visual tweaks
- New player progress bar: a thicker wave that flows while the song plays, a round handle with a little gap on each side, and a dot marking the end.
- Queue: the drag handle now sits next to the cover. Remove has moved into the ⋮ menu (along with "Play now"), so it's harder to tap by accident.
- Playlists: the drag handle also sits next to the cover.

## [0.20.0] — 2026-10-17

### ✨ New
- **Your library updates by itself.** Downloaded a new song, or deleted one? It shows up (or disappears) in a moment, without closing the app.
- **Settings → Library:**
  - **Where your music comes from:** the whole phone, or only the folders you choose (subfolders included).
  - **Minimum length:** a slider to hide audio shorter than a few seconds, like voice notes and ringtones.

### 🎨 Visual tweaks
- Song and album names under covers are no longer clipped at the rounded corners (profile, Releases, Library).
- Your profile's listening time and peak hours are now stacked full width, so nothing is cut off at the edge.
- Release filters wrap to a new line instead of disappearing past the edge of the screen.

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
