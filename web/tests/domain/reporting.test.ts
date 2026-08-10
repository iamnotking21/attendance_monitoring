import { describe, expect, it } from "vitest";

import type { AttendanceRecord, AttendanceStatus } from "@/domain/model";
import {
  attendanceRate,
  buildDashboard,
  entriesByGender,
  entriesByStatus,
  formatRate,
  summariseStudents,
  tally,
} from "@/domain/reporting";

import { makeStudent } from "../fixtures";

function makeRecord(
  studentNumber: string,
  status: AttendanceStatus,
  date: string,
  id = 1,
): AttendanceRecord {
  return {
    id,
    scheduleId: 1,
    sectionId: 1,
    studentNumber,
    date,
    status,
    scheduleTitle: "Morning Assembly",
    recordedAt: `${date}T07:10:00.000Z`,
  };
}

describe("tally and attendanceRate", () => {
  it("counts each status", () => {
    const counts = tally([
      makeRecord("a", "present", "2024-03-01", 1),
      makeRecord("a", "late", "2024-03-02", 2),
      makeRecord("a", "absent", "2024-03-03", 3),
      makeRecord("a", "present", "2024-03-04", 4),
    ]);

    expect(counts).toEqual({ present: 2, late: 1, absent: 1 });
  });

  it("counts late as attending, not as an absence", () => {
    // A student who is late every single day attended every single day. Folding late into
    // absent here was a real reporting error in the original app.
    expect(attendanceRate({ present: 0, late: 10, absent: 0 })).toBe(1);
    expect(attendanceRate({ present: 5, late: 3, absent: 2 })).toBe(0.8);
  });

  it("reports zero rather than dividing by zero when nothing was recorded", () => {
    expect(attendanceRate({ present: 0, late: 0, absent: 0 })).toBe(0);
  });

  it("formats as a whole percentage", () => {
    expect(formatRate(0.8)).toBe("80%");
    expect(formatRate(0.876)).toBe("88%");
    expect(formatRate(0)).toBe("0%");
  });
});

describe("summariseStudents", () => {
  const students = [
    makeStudent({ id: 1, studentNumber: "s1", lastName: "Alvarez", firstName: "Ana" }),
    makeStudent({ id: 2, studentNumber: "s2", lastName: "Bautista", firstName: "Ben" }),
    makeStudent({ id: 3, studentNumber: "s3", lastName: "Castro", firstName: "Cara" }),
  ];

  const records = [
    makeRecord("s1", "present", "2024-03-01", 1),
    makeRecord("s1", "late", "2024-03-02", 2),
    makeRecord("s2", "absent", "2024-03-01", 3),
    // Outside the range under test.
    makeRecord("s1", "present", "2024-02-28", 4),
    makeRecord("s2", "present", "2024-03-10", 5),
  ];

  const range = { start: "2024-03-01", end: "2024-03-05" };

  it("counts only records inside the range, inclusive of both ends", () => {
    const summaries = summariseStudents(students, records, range);
    const byNumber = new Map(summaries.map((s) => [s.student.studentNumber, s]));

    expect(byNumber.get("s1")?.counts).toEqual({ present: 1, late: 1, absent: 0 });
    expect(byNumber.get("s2")?.counts).toEqual({ present: 0, late: 0, absent: 1 });
  });

  it("keeps students who have no records at all", () => {
    // The student nobody ever scanned is precisely the one a coordinator is looking for.
    const summaries = summariseStudents(students, records, range);
    const cara = summaries.find((s) => s.student.studentNumber === "s3");

    expect(cara).toBeDefined();
    expect(cara?.counts).toEqual({ present: 0, late: 0, absent: 0 });
    expect(cara?.sessions).toBe(0);
  });

  it("excludes archived students", () => {
    const withArchived = [...students, makeStudent({ id: 4, studentNumber: "s4", archived: true })];
    const summaries = summariseStudents(withArchived, records, range);

    expect(summaries.map((s) => s.student.studentNumber)).not.toContain("s4");
  });

  it("sorts by display name", () => {
    const summaries = summariseStudents(students, records, range);
    expect(summaries.map((s) => s.student.studentNumber)).toEqual(["s1", "s2", "s3"]);
  });
});

describe("buildDashboard", () => {
  const students = [
    makeStudent({ id: 1, studentNumber: "s1", lastName: "Alvarez", gender: "female" }),
    makeStudent({ id: 2, studentNumber: "s2", lastName: "Bautista", gender: "male" }),
    makeStudent({ id: 3, studentNumber: "s3", lastName: "Castro", gender: "male" }),
  ];

  const date = "2024-03-15";
  const records = [
    makeRecord("s1", "present", date, 1),
    makeRecord("s2", "late", date, 2),
    makeRecord("s1", "present", "2024-03-14", 3),
  ];

  it("counts only the chosen day", () => {
    const breakdown = buildDashboard(students, records, date);
    expect(breakdown.counts).toEqual({ present: 1, late: 1, absent: 0 });
  });

  it("lists students with no record at all as unaccounted for", () => {
    const breakdown = buildDashboard(students, records, date);
    expect(breakdown.unaccountedFor.map((s) => s.studentNumber)).toEqual(["s3"]);
  });

  it("drops records whose student has since been removed", () => {
    const orphaned = [...records, makeRecord("gone", "present", date, 9)];
    const breakdown = buildDashboard(students, orphaned, date);

    expect(breakdown.entries).toHaveLength(2);
    expect(breakdown.counts.present).toBe(1);
  });

  it("splits by status and gender", () => {
    const breakdown = buildDashboard(students, records, date);

    expect(entriesByStatus(breakdown, "present").map((e) => e.student.studentNumber)).toEqual([
      "s1",
    ]);
    expect(
      entriesByGender(entriesByStatus(breakdown, "late"), "male").map(
        (e) => e.student.studentNumber,
      ),
    ).toEqual(["s2"]);
    expect(entriesByGender(breakdown.entries, "female")).toHaveLength(1);
  });
});
