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
- **Buttons in the cover's color**: the player and mini player buttons take the color of the song's cover (can be turned off)
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

## Privacy and permissions

Your library, playlists, favorites, profile, followed artists and stats stay **on your phone only**. There is no account, no ads, no analytics and no server of ours keeping your data. The app only goes online for:

- public artist, album and lyrics information (Deezer, MusicBrainz, Cover Art Archive, Last.fm and LRCLIB), unless offline mode is on
- checking GitHub for a new version

It asks for access to your music and audio, and (on Android 13 and newer) for permission to show release alerts, which you can turn off. The full details are in the [Privacy Policy](https://rilclesaraiva.github.io/BoomListener/privacy).

## 🤖 Built with AI ("vibe coding")

In the spirit of transparency: **Boom, listener! is developed with heavy use of AI.**
Most of the code is written by [Claude](https://claude.ai) (Anthropic) under the
direction of the human maintainer, a workflow often called *vibe coding*.
We take care that it doesn't turn into AI slop:

- **The maintainer decides, the AI executes.** Every feature starts from a real
  need, and every build is tested on a real Android phone before the next round.
- **Written rules the AI has to follow.** [`CLAUDE.md`](CLAUDE.md) and
  [`ARCHITECTURE.md`](ARCHITECTURE.md) (in Portuguese) set the architecture,
  privacy limits and process, and are updated with every decision.
- **Tests and automated builds.** Every change goes through GitHub Actions,
  and a failing test means no build.
- **No release without approval.** Nothing is published without the
  maintainer's explicit approval.
- **Honest changelogs.** [`CHANGELOG.md`](CHANGELOG.md) says what changed,
  including what is still broken.

If AI-assisted code is a concern for you, now you know, and you are welcome to
read every line. Found something careless? Please [open an issue](../../issues).

## Download

Get the APK from [Releases](../../releases) and install it on your phone (Android 8.0 or newer). The app tells you when a new version is out.

Built with Kotlin, Jetpack Compose (Material 3) and Media3, and signed automatically by GitHub Actions.

## License

[GNU GPL v3](LICENSE).

## Legal

- [Privacy Policy](https://rilclesaraiva.github.io/BoomListener/privacy)
- [Terms of Use](https://rilclesaraiva.github.io/BoomListener/terms)

## ☕ Support the project

Boom, listener! is free and maintained independently. The best way to support it is to use it, star the repository and report what breaks.

**Contact:** [open an issue](../../issues).
