---
name: attendance-domain
description: Authoritative reference for the Attendance Monitoring domain — entities, attendance-window state machine, Present/Late/Absent rules, and the legacy Android SQLite schema this project was ported from. Load before changing any attendance, schedule, scanning, or reporting logic.
---

# Attendance Monitoring — domain reference

Ported from the original Android/SQLite application in `mobile-android/`. Rules below are the
behavior the web app must preserve.

## Entities

| Entity | Key fields | Notes |
|---|---|---|
| `Section` | `id`, `name` | A class/section. Soft-deleted. |
| `Student` | `id`, `sectionId`, `studentNumber`, `lastName`, `firstName`, `middleName`, `gender` | `studentNumber` is the QR payload and is unique among active students. Soft-deleted. |
| `Schedule` | `id`, `sectionId`, `title`, `venue`, `presentWindow`, `lateWindow` | Two consecutive time windows. Soft-deleted. |
| `AttendanceRecord` | `id`, `scheduleId`, `sectionId`, `studentNumber`, `recordDate`, `status` | One record per (student, schedule, date). Immutable once written. |
| `SchoolDay` | `date` | Every date the app has been opened on. Drives report date ranges and daily window resets. |

Legacy table/column mapping: `sections.sections_name`, `students.student_number`,
`schedule.start_time` / `end_time` (present window), `schedule.start_time_late` /
`end_time_late` (late window), `attendance_record.status_record`, `days.current_day`.

Legacy encodes soft deletion as the string `"1"` (active) / `"0"` (deleted) in
`status_section`, `student_status`, `status_schedule`. The web port uses typed booleans/enums;
the string form survives only inside the legacy-import adapter.

## Window state machine

Each schedule has four daily flags. Legacy stored `"wala"` (Tagalog: *none*) for "not yet
reached today", and `"1"` / `"0"` for open/closed.

```
                presentWindow.start          presentWindow.end
                       │                            │
   IDLE ───────────────▶ PRESENT_OPEN ──────────────▶ ──┐
                                                        │
                lateWindow.start             lateWindow.end
                       │                            │
                       ▼                            ▼
                   LATE_OPEN ───────────────────────▶ CLOSED
                                                        │
                                          on close: absentee sweep
```

- `IDLE` — before the present window opens. A scan records nothing.
- `PRESENT_OPEN` — a scan records **Present**.
- `LATE_OPEN` — a scan records **Late**.
- `CLOSED` — a scan records nothing. On entering `CLOSED`, every active student in the section
  with no record for this (schedule, date) is written as **Absent**.

All four flags reset to `IDLE` at the start of each new calendar day (legacy:
`refresh_status_start_late`, triggered when a new `current_day` row is inserted).

## Scan rules

1. Resolve `studentNumber` from the QR payload. Unknown or inactive → reject, record nothing.
2. Find every active schedule for that student's section.
3. For each schedule, if a record already exists for (studentNumber, scheduleId, today) → skip.
   **Duplicate scans are never double-counted.**
4. Otherwise write a record with the status implied by the schedule's current window state.
5. A student in no active section, or a section with no active schedule, produces no record.

## Reporting

Per student over an inclusive date range: counts of `Present`, `Late`, `Absent`, plus
attendance rate `(present + late) / total school days in range`.

Section summary splits present/late/absent by gender — the legacy app rendered separate
boys/girls lists on the dashboard.

## Invariants

- One record per (studentNumber, scheduleId, date). Enforce at the repository layer.
- Records are append-only. Correcting a mistake writes a new record; it never mutates history.
- Deleting a section, student, or schedule is a soft delete. Historical records survive it.
- Dates are stored as `YYYY-MM-DD` in the device's local timezone. Never as UTC timestamps —
  a school day is a local-calendar concept, and UTC conversion shifts records across midnight.
- Times are stored as 24-hour `HH:mm`. The legacy `hh:mm:AM/PM` format is parsed only on import.
