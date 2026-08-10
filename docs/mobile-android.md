# The Android app

`mobile-android/` — Kotlin, Jetpack Compose, Room. A rewrite, not a port: the 2019 Java app in
`legacy-android/` cannot build or ship, and the reasons are structural rather than cosmetic
([`android-legacy.md`](android-legacy.md)).

The same domain rules the web app enforces, on a phone, offline, syncing to the same server.

## Building it

```bash
cd mobile-android
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # 56 unit tests
./gradlew lintDebug            # Android lint
./gradlew assembleRelease      # R8-minified release APK
```

Needs JDK 17 or newer and an Android SDK with platform 35. The Gradle wrapper fetches Gradle
itself. Put the SDK path in `mobile-android/local.properties`, escaping the colon:

```properties
sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk
```

| Output | Size |
|---|---|
| `app-debug.apk` | 44.7 MB |
| `app-release-unsigned.apk` | 24.2 MB |

Most of that is ML Kit's bundled barcode model. It is bundled deliberately: the unbundled variant
downloads the model through Play Services on first use, which is exactly the moment a coordinator
is standing at a gate with no signal.

## Structure

```
domain/    Pure Kotlin. Entities, window state machine, reporting, validation.
           No Android imports, so it runs as plain JVM tests with no emulator.
data/      Room entities, DAOs, repository, demo seed, device settings.
sync/      Wire protocol, OkHttp client, replication engine.
ui/        Compose screens, theme, navigation.
```

Dependencies point inward. `domain/` is the same set of rules as `web/src/domain`, expressed in
Kotlin — the window state machine, per-schedule duplicate suppression, the absentee sweep, and
last-write-wins with an id tiebreak all behave identically on both clients, and both test suites
assert the same boundaries.

## The version matrix, and why it is not the newest

```
AGP 8.13.2  ·  Kotlin 2.1.21  ·  KSP 2.1.21-2.0.2  ·  compileSdk 35  ·  minSdk 26
```

Newer was tried first and does not work together yet:

- **AGP 9** bundles its own Kotlin compiler and refuses to run alongside KSP, which Room needs for
  code generation. Its own error says so: *"KSP is not compatible with Android Gradle Plugin's
  built-in Kotlin."* Disabling the built-in compiler then fails differently, because Kotlin 2.2's
  plugin cannot drive AGP 9's extension types.
- **Kotlin 2.2 with AGP 8** compiles, but the Compose BOM of the same vintage ships metadata
  version 2.3, which that compiler cannot read.
- **Ktor 3.4** is built with Kotlin 2.3 metadata for the same reason, so it was replaced with
  **OkHttp**, which has no such coupling and does everything this app asks of it.

The choice was Room or the newest plugins. Room won, and the matrix above is the newest
combination where every piece agrees. Revisit when KSP supports AGP's built-in Kotlin.

`minSdk 26` gives `java.time` without desugaring. The legacy app targeted 22 and paid for it.

## Security, against the legacy app

The old app's security problems were structural, and none of them survive here.

| Legacy | Now |
|---|---|
| Eleven `BroadcastReceiver`s exported without a permission — any installed app could broadcast attendance for any student | No receivers at all. One activity, exported only as the launcher. |
| SQL assembled by string concatenation in three search methods | Room, with bound parameters throughout |
| `WRITE_SETTINGS`, `WRITE_EXTERNAL_STORAGE`, `SET_ALARM`, `RECEIVE_BOOT_COMPLETED` | `CAMERA` and `INTERNET`, and the app works with the latter denied |
| `requestLegacyExternalStorage` for CSV export | Cache directory plus a `FileProvider` and the system share sheet — no storage permission |
| Alarms scheduled with `AlarmManager`, needing re-arming after boot | Windows computed from the clock; nothing to miss and nothing to restore |

Also here and not there: `cleartextTrafficPermitted="false"`, so a bearer token can never travel
over plain HTTP; cloud backup and device transfer excluded, so a roster of minors is not copied
off the device by the platform; and the QR code carries the student number alone — no name, no
section.

The scanner asks for the camera the first time it is opened, not on launch, and a denial leaves
the typed-entry path working.

## Offline and sync

Every screen reads Room. Sync is additive and never sits between a screen and its data, so with
no network the app behaves exactly as it does with one — which is the normal case at a school
gate, not the exception.

Connecting is the same flow as the web client: create a workspace and get a join code, or enter
one. The device receives its own token, so a lost phone can be revoked without disturbing the
others. Conflicts resolve by last-write-wins with an id tiebreak; attendance records never
conflict, being append-only and deduplicated by (student, schedule, date) — enforced by a unique
index in Room, and again in Postgres.

Full protocol: [`sync.md`](sync.md).

## What is not done

- **No instrumented tests.** The domain has 56 unit tests; the Compose screens and the Room DAOs
  have none, because running them needs an emulator this build has never had. That is a real gap,
  not an oversight — the UI is verified only by the compiler and by the release build.
- **The app has never run on a device.** It builds, lints, and passes its unit tests. Nobody has
  yet held a phone to a QR code with it.
- **No signing config**, so the release APK is unsigned and cannot be installed as-is.
