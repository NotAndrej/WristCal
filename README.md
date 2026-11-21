# WristCal

A small, standalone calendar for Wear OS. It shows the events your watch
already keeps in sync with your phone as a scrollable agenda — one flick of the
crown away, right on your wrist.

No account on the watch. No network. No ads, no analytics, no tracking.

## Features

- **Agenda view** — upcoming events grouped by day, with "Today" and
  "Tomorrow" headers, on the round screen.
- **Event details** — tap any event for its time, location, calendar and notes.
- **All-day aware** — all-day events are labelled instead of given a bogus time.
- **Calendar colour dots** — each event carries a dot in its calendar's colour.
- **Refreshes on resume** — the agenda re-reads the calendar whenever you come
  back to the app.
- Works **standalone**, with no phone companion app.

## How it works

Wear OS mirrors the calendars from the paired phone into the on-watch
`CalendarContract` provider. WristCal is a plain client of that provider: it
asks for `READ_CALENDAR` and reads the next 90 days of occurrences. There is no
server, no Google account on the watch, and no data leaves the device.

If the agenda is empty, the watch is not syncing any calendars yet — enable
**Calendar** sync in the watch's system settings (or add a calendar on the
paired phone) and tap **Refresh**.

## Install

Grab the signed APK from the [releases page](../../releases) and sideload it:

```sh
adb install wristcal-1.0.apk
```

The APK targets 32-bit ARM (`armeabi-v7a`), which is what most Wear OS 5
watches — including the Xiaomi Watch 2 — use. Minimum supported version is
Wear OS 3 (API 30).

## Building

```sh
./gradlew :app:assembleDebug     # debug build
./gradlew :app:assembleRelease   # minified release build
```

A release build is signed only when a keystore is supplied out of band, via
either environment variables or Gradle properties:

| Env var                    | Gradle property             |
| -------------------------- | --------------------------- |
| `WRISTCAL_STORE_FILE`      | `wristcal.storeFile`        |
| `WRISTCAL_STORE_PASSWORD`  | `wristcal.storePassword`    |
| `WRISTCAL_KEY_ALIAS`       | `wristcal.keyAlias`         |
| `WRISTCAL_KEY_PASSWORD`    | `wristcal.keyPassword`      |

With no keystore configured the release APK is still produced, just unsigned —
which is what reproducible builds want.

Requires JDK 17+, Android SDK with build tools for API 36, and the Android SDK
platform 36.

## Tech

Kotlin, Jetpack Compose for Wear OS, a single `CalendarContract` query, and
nothing else. The launcher icon and all in-app glyphs are hand-authored
vectors.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
