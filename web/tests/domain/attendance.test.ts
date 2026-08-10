import { describe, expect, it } from "vitest";

import {
  absentRecordsFor,
  isCollectingScans,
  nextTransitionMinute,
  resolveScan,
  statusForWindow,
  windowStateAt,
} from "@/domain/attendance";
import { recordKey } from "@/domain/model";

import { AT, makeSchedule } from "../fixtures";

const RECORDED_AT = "2024-03-15T07:15:00.000Z";
const DATE = "2024-03-15";

describe("windowStateAt", () => {
  const schedule = makeSchedule();

  it.each([
    [AT.before, "before"],
    [AT.presentStart, "present"],
    [AT.presentMiddle, "present"],
    [AT.presentEnd, "late"],
    [AT.lateMiddle, "late"],
    [AT.lateEnd, "closed"],
    [AT.after, "closed"],
  ])("is %s minutes -> %s", (minute, expected) => {
    expect(windowStateAt(schedule, minute)).toBe(expected);
  });

  it("treats each window as start-inclusive and end-exclusive", () => {
    // The boundary minute belongs to the window that is opening, never to the one closing.
    // Getting this backwards would let a student scan at exactly 07:30 and be marked present
    // when the present window has already shut.
    expect(windowStateAt(schedule, AT.presentStart)).toBe("present");
    expect(windowStateAt(schedule, AT.presentEnd)).toBe("late");
    expect(windowStateAt(schedule, AT.lateEnd)).toBe("closed");
  });

  it("reports a gap when the schedule leaves a deliberate pause", () => {
    const gapped = makeSchedule({ late: { start: "08:00", end: "08:30" } });
    expect(windowStateAt(gapped, 7 * 60 + 45)).toBe("gap");
    expect(statusForWindow("gap")).toBeNull();
  });
});

describe("statusForWindow", () => {
  it("only the two open windows earn a status", () => {
    expect(statusForWindow("present")).toBe("present");
    expect(statusForWindow("late")).toBe("late");
    expect(statusForWindow("before")).toBeNull();
    expect(statusForWindow("gap")).toBeNull();
    expect(statusForWindow("closed")).toBeNull();
  });
});

describe("nextTransitionMinute", () => {
  const schedule = makeSchedule();

  it("finds the next boundary ahead of the given time", () => {
    expect(nextTransitionMinute(schedule, 0)).toBe(AT.presentStart);
    expect(nextTransitionMinute(schedule, AT.presentMiddle)).toBe(AT.presentEnd);
    expect(nextTransitionMinute(schedule, AT.lateMiddle)).toBe(AT.lateEnd);
  });

  it("returns null once the schedule is finished for the day", () => {
    expect(nextTransitionMinute(schedule, AT.lateEnd)).toBeNull();
  });

  it("agrees with isCollectingScans", () => {
    expect(isCollectingScans(schedule, AT.presentMiddle)).toBe(true);
    expect(isCollectingScans(schedule, AT.lateMiddle)).toBe(true);
    expect(isCollectingScans(schedule, AT.after)).toBe(false);
  });
});

describe("resolveScan", () => {
  const base = {
    studentNumber: "2024-1001",
    date: DATE,
    recordedAt: RECORDED_AT,
    existingKeys: new Set<string>(),
  };

  it("records present inside the present window", () => {
    const result = resolveScan({
      ...base,
      schedules: [makeSchedule()],
      atMinutes: AT.presentMiddle,
    });

    expect(result.created).toHaveLength(1);
    expect(result.created[0]).toMatchObject({
      status: "present",
      scheduleId: 1,
      sectionId: 1,
      studentNumber: "2024-1001",
      date: DATE,
      scheduleTitle: "Morning Assembly",
    });
  });

  it("records late inside the late window", () => {
    const result = resolveScan({
      ...base,
      schedules: [makeSchedule()],
      atMinutes: AT.lateMiddle,
    });

    expect(result.created).toHaveLength(1);
    expect(result.created[0].status).toBe("late");
  });

  it("records nothing when both windows are shut", () => {
    for (const minute of [AT.before, AT.after]) {
      const result = resolveScan({ ...base, schedules: [makeSchedule()], atMinutes: minute });
      expect(result.created).toEqual([]);
      expect(result.inactiveScheduleIds).toEqual([1]);
    }
  });

  it("suppresses a second scan for a schedule already recorded today", () => {
    const existing = new Set([
      recordKey({ studentNumber: "2024-1001", scheduleId: 1, date: DATE }),
    ]);

    const result = resolveScan({
      ...base,
      existingKeys: existing,
      schedules: [makeSchedule()],
      atMinutes: AT.presentMiddle,
    });

    expect(result.created).toEqual([]);
    expect(result.duplicateScheduleIds).toEqual([1]);
  });

  it("does not suppress the same student on a different day", () => {
    const existing = new Set([
      recordKey({ studentNumber: "2024-1001", scheduleId: 1, date: "2024-03-14" }),
    ]);

    const result = resolveScan({
      ...base,
      existingKeys: existing,
      schedules: [makeSchedule()],
      atMinutes: AT.presentMiddle,
    });

    expect(result.created).toHaveLength(1);
  });

  it("records every open schedule from a single scan", () => {
    const result = resolveScan({
      ...base,
      schedules: [
        makeSchedule({ id: 1, title: "Assembly" }),
        makeSchedule({ id: 2, title: "Homeroom" }),
      ],
      atMinutes: AT.presentMiddle,
    });

    expect(result.created.map((record) => record.scheduleTitle)).toEqual([
      "Assembly",
      "Homeroom",
    ]);
  });

  it("suppresses per schedule, not per scan", () => {
    // Already marked present for the assembly; homeroom is open and still unrecorded, so this
    // scan must record homeroom without touching the assembly.
    const existing = new Set([
      recordKey({ studentNumber: "2024-1001", scheduleId: 1, date: DATE }),
    ]);

    const result = resolveScan({
      ...base,
      existingKeys: existing,
      schedules: [
        makeSchedule({ id: 1, title: "Assembly" }),
        makeSchedule({ id: 2, title: "Homeroom" }),
      ],
      atMinutes: AT.presentMiddle,
    });

    expect(result.duplicateScheduleIds).toEqual([1]);
    expect(result.created).toHaveLength(1);
    expect(result.created[0].scheduleId).toBe(2);
  });

  it("ignores archived schedules entirely", () => {
    const result = resolveScan({
      ...base,
      schedules: [makeSchedule({ archived: true })],
      atMinutes: AT.presentMiddle,
    });

    expect(result.created).toEqual([]);
    expect(result.inactiveScheduleIds).toEqual([]);
    expect(result.duplicateScheduleIds).toEqual([]);
  });
});

describe("absentRecordsFor", () => {
  const base = {
    schedule: makeSchedule(),
    date: DATE,
    recordedAt: RECORDED_AT,
    activeStudentNumbers: ["2024-1001", "2024-1002", "2024-1003"],
    existingKeys: new Set<string>(),
  };

  it("marks everyone unrecorded absent once the late window closes", () => {
    const absentees = absentRecordsFor({ ...base, atMinutes: AT.after });

    expect(absentees).toHaveLength(3);
    expect(absentees.every((record) => record.status === "absent")).toBe(true);
    expect(absentees.map((record) => record.studentNumber)).toEqual(base.activeStudentNumbers);
  });

  it("skips students who already have a record", () => {
    const existingKeys = new Set([
      recordKey({ studentNumber: "2024-1002", scheduleId: 1, date: DATE }),
    ]);

    const absentees = absentRecordsFor({ ...base, existingKeys, atMinutes: AT.after });

    expect(absentees.map((record) => record.studentNumber)).toEqual([
      "2024-1001",
      "2024-1003",
    ]);
  });

  it("refuses to sweep while the schedule is still open", () => {
    for (const minute of [AT.before, AT.presentMiddle, AT.lateMiddle]) {
      expect(absentRecordsFor({ ...base, atMinutes: minute })).toEqual([]);
    }
  });

  it("sweeps at the exact minute the late window closes", () => {
    expect(absentRecordsFor({ ...base, atMinutes: AT.lateEnd })).toHaveLength(3);
  });

  it("is idempotent — a second sweep finds nothing left", () => {
    const first = absentRecordsFor({ ...base, atMinutes: AT.after });

    const afterFirst = new Set(first.map(recordKey));
    const second = absentRecordsFor({ ...base, existingKeys: afterFirst, atMinutes: AT.after });

    expect(second).toEqual([]);
  });

  it("ignores archived schedules", () => {
    const archived = { ...base, schedule: makeSchedule({ archived: true }) };
    expect(absentRecordsFor({ ...archived, atMinutes: AT.after })).toEqual([]);
  });
});
