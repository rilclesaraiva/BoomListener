# Boom, listener!

A **local** music player for Android. Free, no ads, no account.

> 🚧 Work in progress. Test builds come out of every update in the Actions tab.

## What it does

- Plays the music on your device, with a **Material 3 Expressive** look
- Mini player, full-screen player with a wavy progress bar, queue, shuffle and repeat
- Keeps playing in the background, with notification, lock screen and headphone controls
- Light, dark and AMOLED themes, plus your wallpaper colors
- English and Portuguese

### Coming next

- **Full tag editing**: title, artist, album, year, genre, cover and lyrics
- **Artist page** with photo, biography and discography, marking what you have and what you're missing
- **Release radar**: follow artists and get notified about new music
- **Wellbeing**: listening stats and gentle break reminders

The app **never streams** from external services. Deezer and Last.fm are used only for information, and new releases come with a button to open them in the service you prefer.

## Privacy

Your library, followed artists and stats stay **on your device only**. The app only contacts Deezer and Last.fm for public artist and album information, GitHub only when you tap "Check for updates", and sends what you listen to (scrobbling) to Last.fm only if you turn it on.

## Tech

Kotlin, Jetpack Compose and Material 3, split into modules (`core`, `data`, `feature`). See [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) (in Portuguese). Every update is built and signed automatically by GitHub Actions.

## License

[GNU GPL v3](LICENSE). Written with AI assistance, under the author's direction and review.
