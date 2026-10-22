# Boom, listener!

A **local** music player for Android. Free, open source, no ads, no account.

> 🧪 **Beta.** The app works day to day, but it is still being polished. The phone in portrait is the main target today. **Landscape and tablet layouts still have broken spots** that we know about and are fixing. Bug reports are welcome in [Issues](../../issues).

## What it does

- **Plays the music on your phone**, with a Material 3 Expressive look: mini player, full-screen player with a wavy progress bar, queue you can reorder, shuffle and repeat, synced lyrics, sleep timer, playback speed and crossfade
- **Swipe to change songs** on the mini player or on the cover
- **Edit tags**: title, artist, album, year, genre, cover and lyrics, with automatic suggestions from Deezer and MusicBrainz
- **Artist pages** with photo, biography (Last.fm), popular songs and the full discography, marking what you have and what you're missing
- **Releases**: follow artists and see their new albums, EPs and singles in one place, with an alert on your phone when something new comes out
- **Favorites and playlists**
- **Your profile**, just for you: name, photo, cover (GIFs too) and color, with your listening stats, most played songs, top artists and history. It never leaves your phone.
- Background playback with notification, lock screen and headphone controls; choose the audio output from the player
- Light, dark and AMOLED themes, plus your wallpaper colors
- English and Portuguese

The app **never streams** from external services. Deezer, MusicBrainz and Last.fm are used only for information, and outside links open in the service you choose.

## Privacy

Your library, playlists, favorites, profile, followed artists and stats stay **on your phone only**. There is no account and no server of ours keeping your data. The app only goes online for:

- public artist, album and lyrics information (Deezer, MusicBrainz, Cover Art Archive, Last.fm and LRCLIB)
- matching YouTube Music links when you share a song (song.link)
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
