import { afterEach, beforeEach, describe, expect, it } from "vitest";

import type { AttendanceDatabase } from "@/lib/db";
import { listRecordsBySectionAndDate } from "@/lib/repositories/records";
import { createSection } from "@/lib/repositories/sections";
import { createSchedule } from "@/lib/repositories/schedules";
import { createStudent } from "@/lib/repositories/students";
import { openDay, recordScan, sweepAbsentees } from "@/lib/services/attendance";
import { exportBackup, resetDatabase, restoreBackupFromJson } from "@/lib/services/backup";

import { closeDatabase, freshDatabase } from "../helpers/db";

let database: AttendanceDatabase;
let sectionId: number;

/** 15 March 2024, at a given local wall-clock time. */
function at(hours: number, minutes = 0): Date {
  return new Date(2024, 2, 15, hours, minutes, 0);
}

const DATE = "2024-03-15";

beforeEach(async () => {
  database = await freshDatabase();

  sectionId = await createSection({ name: "Grade 11 - Rizal" });
  await createSchedule({
    sectionId,
    title: "Morning Assembly",
    venue: "Quadrangle",
    present: { start: "07:00", end: "07:30" },
    late: { start: "07:30", end: "08:00" },
  });

  for (const [number, lastName] of [
    ["2024-1001", "Dela Cruz"],
    ["2024-1002", "Santos"],
    ["2024-1003", "Reyes"],
  ]) {
    await createStudent({
      sectionId,
      studentNumber: number,
      lastName,
      firstName: "Test",
      middleName: "",
      gender: "male",
    });
  }
});

afterEach(async () => {
  await closeDatabase(database);
});

describe("recordScan", () => {
  it("records present inside the present window", async () => {
    const result = await recordScan("2024-1001", at(7, 15));

    expect(result.kind).toBe("recorded");
    if (result.kind === "recorded") {
      expect(result.records[0].status).toBe("present");
      expect(result.student.lastName).toBe("Dela Cruz");
    }
  });

  it("records late inside the late window", async () => {
    const result = await recordScan("2024-1001", at(7, 45));

    expect(result.kind).toBe("recorded");
    if (result.kind === "recorded") expect(result.records[0].status).toBe("late");
  });

  it("reports a duplicate on the second scan and stores only one record", async () => {
    await recordScan("2024-1001", at(7, 15));
    const second = await recordScan("2024-1001", at(7, 20));

    expect(second.kind).toBe("duplicate");
    expect(await database.records.count()).toBe(1);
  });

  it("keeps the earlier status when a student rescans after the window moves on", async () => {
    // Present at 07:15, then scanned again at 07:45. They were on time; the late window must
    // not overwrite that.
    await recordScan("2024-1001", at(7, 15));
    await recordScan("2024-1001", at(7, 45));

    const records = await listRecordsBySectionAndDate(sectionId, DATE);
    expect(records).toHaveLength(1);
    expect(records[0].status).toBe("present");
  });

  it("records nothing outside both windows", async () => {
    const early = await recordScan("2024-1001", at(6, 30));
    expect(early.kind).toBe("closed");
    expect(await database.records.count()).toBe(0);
  });

  it("rejects an unknown student number", async () => {
    const result = await recordScan("2024-9999", at(7, 15));
    expect(result.kind).toBe("unknown");
    expect(await database.records.count()).toBe(0);
  });

  it("rejects a malformed payload without touching storage", async () => {
    for (const payload of ["<script>alert(1)</script>", "https://evil.example", ""]) {
      const result = await recordScan(payload, at(7, 15));
      expect(result.kind).toBe("malformed");
    }
    expect(await database.records.count()).toBe(0);
  });

  it("registers the day it recorded on", async () => {
    await recordScan("2024-1001", at(7, 15));
    expect(await database.schoolDays.get(DATE)).toBeDefined();
  });
});

describe("sweepAbsentees", () => {
  it("does nothing while the schedule is still open", async () => {
    expect(await sweepAbsentees(at(7, 45))).toBe(0);
    expect(await database.records.count()).toBe(0);
  });

  it("marks everyone unscanned absent once the late window closes", async () => {
    await recordScan("2024-1001", at(7, 15));

    const written = await sweepAbsentees(at(8, 5));

    expect(written).toBe(2);
    const records = await listRecordsBySectionAndDate(sectionId, DATE);
    expect(records.filter((r) => r.status === "absent").map((r) => r.studentNumber).sort()).toEqual(
      ["2024-1002", "2024-1003"],
    );
    expect(records.find((r) => r.studentNumber === "2024-1001")?.status).toBe("present");
  });

  it("is idempotent across repeated runs", async () => {
    await sweepAbsentees(at(8, 5));
    const secondRun = await sweepAbsentees(at(9, 0));

    expect(secondRun).toBe(0);
    expect(await database.records.count()).toBe(3);
  });

  it("never marks an archived student absent", async () => {
    await database.students.where({ studentNumber: "2024-1003" }).modify({ archived: true });

    await sweepAbsentees(at(8, 5));

    const records = await listRecordsBySectionAndDate(sectionId, DATE);
    expect(records.map((r) => r.studentNumber)).not.toContain("2024-1003");
  });

  it("openDay registers the date and settles the day in one call", async () => {
    await openDay(at(8, 5));

    expect(await database.schoolDays.get(DATE)).toBeDefined();
    expect(await database.records.count()).toBe(3);
  });
});

describe("backup round trip", () => {
  it("restores exactly what it exported", async () => {
    await recordScan("2024-1001", at(7, 15));
    await sweepAbsentees(at(8, 5));

    const before = await exportBackup(at(9, 0));
    const json = JSON.stringify(before);

    await resetDatabase();
    expect(await database.records.count()).toBe(0);

    const result = await restoreBackupFromJson(json);
    expect(result.ok).toBe(true);

    const after = await exportBackup(at(9, 0));
    expect(after.data).toEqual(before.data);
  });

  it("leaves existing data untouched when the file is not a backup", async () => {
    await recordScan("2024-1001", at(7, 15));

    const result = await restoreBackupFromJson('{"format":"something-else"}');

    expect(result.ok).toBe(false);
    expect(await database.records.count()).toBe(1);
  });

  it("rejects a backup carrying a row that fails validation", async () => {
    const backup = await exportBackup(at(9, 0));
    const tampered = {
      ...backup,
      data: {
        ...backup.data,
        students: [{ ...backup.data.students[0], studentNumber: "<script>" }],
      },
    };

    const result = await restoreBackupFromJson(JSON.stringify(tampered));

    expect(result.ok).toBe(false);
    expect(await database.students.count()).toBe(3);
  });
});
