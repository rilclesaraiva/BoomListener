# Boom, listener!

A **local** music player for Android. Free, open source, no ads, no account.

> 🧪 **Beta.** The app works day to day, but it is still being polished. The phone in portrait is the main target today. **Landscape and tablet layouts still have broken spots** that we know about and are fixing. Bug reports are welcome in [Issues](../../issues).

## What it does

### Playing music
- **Plays the music on your phone** with a Material 3 Expressive look: a floating mini player that grows into the full-screen player, a wavy progress bar that moves smoothly, and big expressive controls
- **Swipe to change songs** on the mini player or on the cover
- **Queue** you can reorder, "Play next", shuffle and repeat, and a **Mix** that fills up next with similar songs from your library
- **Synced lyrics** from the file or from LRCLIB (you choose which comes first)
- **Audio visualizer over the cover** (bars or wave), turned on from the player menu. It reads the app's own audio, so it needs no microphone permission
- **Playback options:** crossfade between songs, fade on play/pause, gapless playback, ReplayGain, playback speed, sleep timer, resume when your headphones reconnect, and remember shuffle
- **Picks up where you left off** after closing the app, or stops completely when you swipe it away from recents (optional)
- **Choose the audio output** from the player, with your Bluetooth headphones' custom name
- Background playback with notification, lock screen and headphone controls
- Plays MP3, AAC/M4A, OGG, Opus, WAV and FLAC (ALAC depends on your phone)

### Your library
- Home with recently added songs, albums and artists, plus search
- Library with genres, artists, playlists and albums, each in a list or grid
- **Favorites and playlists**, with a 2×2 cover mosaic for playlists with several albums
- Long-press anything for a menu: play next, add to playlist, go to album or artist, details, share, delete from the phone
- Filter the library by folder and minimum length
- Fast scroller with letter bubble on long lists

### Tags
- **Edit tags**: title, artist, album, year, genre, cover and lyrics
- **Automatic tags** from Deezer, with MusicBrainz and Cover Art Archive as backup and genres from Last.fm. Songs with several artists ("feat.", "x", "&") are matched too
- If your tags are already right, nothing is overwritten. When a different cover is found, you see both side by side and decide

### Artists and releases
- **Artist pages** with photo, biography (Last.fm), popular songs and the full discography, marking what you have and what you're missing
- Songs with more than one artist let you pick which one to open, with their photos
- **Releases**: follow artists and see their new albums, EPs and singles in one place, filtered by type and time
- **Release alerts** on your phone, which you can turn off for everything or just for one artist

### Your profile
- Just for you: name, description, photo, cover (GIFs and short videos too) and color, as a gradient or solid page
- Listening stats, most played songs, listening time, peak hours, top artists and full history
- It never leaves your phone. There are no social features

### Sharing
- Share a song as a YouTube Music search link, or as a **banner** image or a video with up to 30 seconds of the song and a live equalizer
- Set a favorite app to share with in one tap

### And also
- **Backup and restore** everything (favorites, playlists, profile, stats, settings) to a single file. Songs are found again on a new phone
- **Offline mode**: the app never goes online (except to check for updates), and keeps showing what's cached
- Light, dark and AMOLED themes, your wallpaper colors, and the player tinted by the cover
- Background blur behind screens and the player (Android 12+, can be turned off)
- **English, Portuguese and Spanish**, with per-app language on Android 13+
- Tells you when a new version is out, with the release notes. You can pause these notices or skip a version
- Wide layout with a sidebar and an artist panel for tablets and landscape (still rough, see above)

The app **never streams** from external services. Deezer, MusicBrainz and Last.fm are used only for information, and outside links open in the service you choose.

## Privacy

Your library, playlists, favorites, profile, followed artists and stats stay **on your phone only**. There is no account and no server of ours keeping your data. The app only goes online for:

- public artist, album and lyrics information (Deezer, MusicBrainz, Cover Art Archive, Last.fm and LRCLIB), unless offline mode is on
- checking GitHub for a new version

It asks for access to your music and audio, and (on Android 13 and newer) for permission to show release alerts, which you can turn off.

## Download

Get the APK from [Releases](../../releases). The app tells you when a new version is out.

## How it's made: AI-assisted, on purpose

Boom, listener! is built with heavy use of AI. Most of the code is written by Claude (Anthropic), working from the author's ideas, sketches, screenshots and bug reports. You could call it "vibe coding". We take care that it doesn't turn into AI slop:

- **The author decides, the AI executes.** Every feature starts from a real need, and every build is tested on a real phone before the next round. What looks or feels wrong goes back to be fixed.
- **Written rules the AI has to follow.** [`CLAUDE.md`](CLAUDE.md) and [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) (in Portuguese) set the architecture, design system, privacy limits and process. They are updated with every decision.
- **Tests and automated builds.** The logic that matters (shuffle, play counts, tag matching, artist matching, stats, releases) has unit tests. Every change goes through GitHub Actions, and a failing test means no build.
- **Cleanup on purpose.** Features that didn't fit get removed for real, including old code nobody uses, instead of piling up. One example is the social features, which were dropped.
- **Honest changelogs.** [`CHANGELOG.md`](CHANGELOG.md) says what changed, including what is still broken.

If you find something that feels careless, please open an issue.

## Tech

Kotlin, Jetpack Compose and Material 3, split into modules (`core`, `data`, `feature`, `app`). Media3 (ExoPlayer) for playback. Every update is built and signed automatically by GitHub Actions.

## License

[GNU GPL v3](LICENSE).
