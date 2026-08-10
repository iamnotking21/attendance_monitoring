# The legacy Android app

> Lives in `legacy-android/`. The current Android app is a Kotlin and Compose rewrite in
> `mobile-android/` — see [mobile-android.md](mobile-android.md).

Assessed 2026-08-10 · `legacy-android/` · Java, Gradle 5.4.1, AGP 3.5.1, SQLite, ZXing

## Verdict

It cannot be built, cannot be published, and would crash on a current phone. It is kept as
reference for how the attendance rules were originally expressed — the web app in `web/` is the
product, and it runs on the same phones through the browser.

This is not a criticism of the original work. It was written in December 2019 and it worked; the
platform moved.

## Why it cannot be built

| Blocker | Detail |
|---|---|
| `jcenter()` | Both `build.gradle` files resolve dependencies from JCenter, which went read-only in 2021 and was sunset afterwards. Dependency resolution fails outright. |
| Android Gradle Plugin 3.5.1 | Requires JDK 8. Current Android tooling requires JDK 17+, and AGP 3.x is no longer supported by Android Studio. |
| Gradle 5.4.1 | Predates Java 11 support in the wrapper; incompatible with any recent JDK. |
| `buildToolsVersion "29.0.2"` | Not shipped by current SDK installs. |

## Why it could not be published

`targetSdkVersion 29` (Android 10). Google Play has required a far higher target for new uploads
and updates for several years; a build targeting 29 is rejected. Raising it is not a version-number
edit — it pulls in scoped storage, runtime permission changes, and the restrictions below.

## Why it would crash

`CheckingTime.java`, `timer_start.java`, and the other schedulers construct alarms with:

```java
PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
```

From Android 12 (API 31), creating a `PendingIntent` without `FLAG_IMMUTABLE` or `FLAG_MUTABLE`
throws. Every attendance window in the app is scheduled this way, so the first schedule would
take the app down on any phone sold in the last four years.

`android:requestLegacyExternalStorage="true"` in the manifest is also ignored from Android 11,
which breaks the CSV export and the database backup and restore in `Database_main.java`.

## Security findings in the legacy code

These are recorded because the code is public, not because it is running anywhere.

### 1. Every broadcast receiver is exported — HIGH

All eleven receivers in `AndroidManifest.xml` are declared `android:exported="true"` with no
permission guard:

```xml
<receiver android:name=".attendance_record" android:enabled="true" android:exported="true" />
```

`attendance_record` reads `intent.getStringExtra("student_number")` and writes an attendance
record from it. Any other app installed on the device can therefore broadcast to it and mark any
student present, with no user interaction and no special permission. The same applies to
`CheckingTime`, `do_daily_receiver`, and the window receivers, which can be triggered to open or
close attendance windows out of order.

Only `backgroundservice` needs to be exported at all, for `BOOT_COMPLETED`.

### 2. SQL assembled by string concatenation — MEDIUM

`DatabaseHelper.search`, `student_DBManager.search_student`, and
`schedule_DBManager.search_schedule` build queries by concatenation:

```java
String query = "SELECT * FROM students WHERE student_number LIKE '%" + inputText +
  "%' AND section_id = '" + id + "' AND student_status = '1' ";
```

The input is a local search box rather than a remote request, so the practical risk is a user
corrupting their own database — but the rest of the class already uses parameterised
`db.query(...)` calls, so the exposure is avoidable and inconsistent rather than necessary.

### 3. `WRITE_SETTINGS` requested — LOW

A permission to modify global system settings, requested by an attendance app that has no reason
to. It is flagged `tools:ignore="ProtectedPermissions"` in the manifest, which suppresses the
warning rather than answering it.

## What the rewrite carried over, and what it fixed

| Legacy behaviour | In the web app |
|---|---|
| Present/late window state machine driven by AlarmManager flags | `domain/attendance.ts`, computed from the clock — no alarms to miss, no flags to leave stale |
| Absentee sweep at end of late window | `sweepAbsentees`, idempotent and re-runnable |
| One record per student per schedule per day, checked in code | Enforced by a unique index in IndexedDB and again in Postgres |
| Soft deletes as `"1"` / `"0"` strings | Typed booleans |
| `getfuckingschedules`, `getSectionnamebitch`, `una`, `tapos`, `wala` | Domain vocabulary; `wala` became an explicit `ScheduleWindowState` |
| String-concatenated SQL | Parameterised queries and Zod-validated input |
| Exported receivers | No IPC surface at all; the browser is the sandbox |

## The rewrite

It happened. `mobile-android/` is a Kotlin and Compose app on a current toolchain, built from the
same domain rules, with every finding above closed: no exported receivers, no string-built SQL,
two permissions instead of six, and no alarms to re-arm. See
[mobile-android.md](mobile-android.md).

This directory stays as the record of where those rules came from.
