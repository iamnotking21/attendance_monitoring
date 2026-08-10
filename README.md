# Attendance Monitoring

QR-code attendance system for schools. Scan a student ID, and the record lands in the right
schedule with the right status — Present, Late, or Absent — with no double counting.

Originally an Android/SQLite app; being rebuilt as a modern, local-first web application.

## Repository layout

```
web/             Next.js web application (in progress)
mobile-android/  Original Android app — Java + SQLite, preserved as reference
devops/          Dockerfile, compose, CI
docs/            Architecture notes, security audit, ADRs
.claude/         Specialist agent definitions, skills, and the ship pipeline
```

## Domain

Five entities: `Section`, `Student`, `Schedule`, `AttendanceRecord`, `SchoolDay`.

Each schedule owns two consecutive daily windows — a **present** window and a **late** window.
A scan is graded by whichever window is open when it arrives; scans outside both windows record
nothing. When the late window closes, every student in the section without a record for that
schedule and date is swept in as **Absent**.

Records are append-only and unique per (student, schedule, date), so a student scanning twice is
counted once. Deleting a section, student, or schedule is a soft delete — historical records
survive it.

Full rules: [`.claude/skills/attendance-domain/SKILL.md`](.claude/skills/attendance-domain/SKILL.md)

## Legacy app

`mobile-android/` is the original 2019-era Android project — Gradle 5.4.1, Java, SQLite,
ZXing for QR. It is kept for reference and is not built by CI.

## Development

Setup instructions land with the web application.

## License

MIT
