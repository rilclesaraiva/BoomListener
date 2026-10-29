---
title: Privacy Policy
---

# Privacy Policy — Boom, listener!

_Last updated: October 29, 2026_

Boom, listener! ("the app") is a free, open-source music player for Android that plays the music files stored on your phone. This policy explains what the app does with information, based on what its [source code](https://github.com/rilclesaraiva/BoomListener) actually does.

**Who is responsible:** the app is maintained by **rilclesaraiva** ([github.com/rilclesaraiva](https://github.com/rilclesaraiva)). Contact: [open an issue](https://github.com/rilclesaraiva/BoomListener/issues) on the project repository.

## In short

- There is no account, no ads, no analytics and no tracking.
- Your music, playlists, favorites, listening history, stats, profile and settings are stored **only on your phone**. The maintainer has no server that receives them.
- The app goes online only to look up public information (artist photos and biographies, album details, lyrics, tags) and to check for new versions. You can turn this off with **offline mode** (Settings → System), which leaves only the update check.

## What stays on your phone

The app keeps the following in its private storage on your device:

- the list of songs Android gives it (title, artist, album, length, file location) and the covers inside your files;
- your playlists, favorites, play counts, listening history and listening stats;
- your profile (name, description, color, photo and cover), which is only shown to you;
- your settings, the artists you follow and which release alerts you have seen;
- temporary copies of public information downloaded from the services below (a cache), so it doesn't have to be downloaded again.

None of this is sent anywhere. If you make a **backup** (Settings → System → Backup), the app writes a file to the place you choose; what you do with that file is up to you.

## What is sent over the internet, and to whom

Each request goes directly from your phone to the service (except Last.fm, see below). Like any internet request, the service can see your IP address. The app sends only what each lookup needs:

| Service | When | What is sent |
|---|---|---|
| **Deezer** (api.deezer.com and its image servers) | Opening an artist page (or the artist panel on wide screens), choosing between the artists of a song, the profile's "Your artists", searching artists in the Library, fixing tags, and the release check for artists you follow (about twice a day, in the background) | Artist names, song title, artist and length, what you type in the artist search, Deezer IDs of artists you follow |
| **MusicBrainz** and **Cover Art Archive** | When Deezer doesn't know an artist or song | Artist names, song title, artist and length |
| **Last.fm**, through the project's own Cloudflare Worker | Artist biography and song genres | Artist name (and song title for genres). The Worker only adds the project's access key and forwards the request; it runs on Cloudflare, which can see your IP address |
| **LRCLIB** | When you open the lyrics of a song | Song title, artist, album and length |
| **GitHub** | When the app opens (new version check) and in Settings → About (the developer's photo, at most about once a week) | Only a request for the latest release and for the photo |

The app identifies itself to MusicBrainz, LRCLIB and GitHub with its name ("BoomListener"), as those services ask. These services have their own privacy policies.

**Sharing:** when you share a song, the app builds a YouTube Music *search link* (or an image or a short video made on your phone) and hands it to the app you pick. Nothing is looked up online for that.

## Permissions

- **Music and audio** (`READ_MEDIA_AUDIO`, or storage read access on Android 12 and older): to find and play the songs on your phone.
- **Notifications** (Android 13 and newer): only for release alerts, which you can turn off. The playback controls in the notification area don't need it.
- **Internet**: for the lookups listed above.
- **Foreground service (media playback)**: to keep playing with the screen off.
- **Write to storage** (Android 8 and 9 only): to save tags you edit. On Android 10 and newer, Android asks you file by file instead.

The app doesn't ask for photos, videos, contacts, location, microphone or Bluetooth. Photos for your profile or a song cover are picked with Android's photo picker, which gives the app only the image you choose.

## How long data is kept and how to delete it

Everything stays on your phone until you delete it: remove items inside the app, clear the app's data in Android's settings, or uninstall the app. Cached public information is kept for a limited time and is also removed by clearing the app's cache or data. Because the maintainer holds no copy of your data, there is nothing to request from us; if you have a question, [open an issue](https://github.com/rilclesaraiva/BoomListener/issues).

## Your rights (LGPD and GDPR)

Brazil's LGPD and the EU's GDPR give you rights over your personal data, such as access, correction and deletion. Since the app keeps your data only on your device, you exercise them directly in the app or in Android's settings. Requests made to the services above (Deezer, MusicBrainz, Last.fm, LRCLIB, GitHub, Cloudflare) are covered by their own policies.

## Children

The app is not directed at children and collects no personal data from anyone.

## Changes

When what the app does with information changes, this page is updated in the same change, with a new date. The history is public in the [repository](https://github.com/rilclesaraiva/BoomListener/commits/main/docs/privacy.md).
