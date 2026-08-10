import { afterEach, beforeEach, describe, expect, it } from "vitest";

import type { AttendanceDatabase } from "@/lib/db";
import {
  appendRecords,
  existingKeysForDate,
  listRecordsBySectionAndDate,
  listRecordsBySectionBetween,
} from "@/lib/repositories/records";
import {
  DuplicateSectionError,
  archiveSection,
  createSection,
  listSections,
  renameSection,
} from "@/lib/repositories/sections";
import {
  archiveSchedule,
  createSchedule,
  listSchedulesBySection,
} from "@/lib/repositories/schedules";
import { markSchoolDay, listSchoolDays } from "@/lib/repositories/schoolDays";
import {
  DuplicateStudentNumberError,
  archiveStudent,
  createStudent,
  findActiveByStudentNumber,
  listStudentsBySection,
  searchStudents,
} from "@/lib/repositories/students";

import { IDS } from "../fixtures";
import { closeDatabase, freshDatabase } from "../helpers/db";

let database: AttendanceDatabase;

beforeEach(async () => {
  database = await freshDatabase();
});

afterEach(async () => {
  await closeDatabase(database);
});

describe("sections", () => {
  it("creates and lists in alphabetical order", async () => {
    await createSection({ name: "Grade 12 - Mabini" });
    await createSection({ name: "Grade 11 - Rizal" });

    expect((await listSections()).map((s) => s.name)).toEqual([
      "Grade 11 - Rizal",
      "Grade 12 - Mabini",
    ]);
  });

  it("refuses a duplicate name regardless of case", async () => {
    await createSection({ name: "Grade 11 - Rizal" });
    await expect(createSection({ name: "grade 11 - rizal" })).rejects.toBeInstanceOf(
      DuplicateSectionError,
    );
  });

  it("allows renaming a section to its own name", async () => {
    const id = await createSection({ name: "Grade 11 - Rizal" });
    await expect(renameSection(id, { name: "Grade 11 - Rizal" })).resolves.toBeUndefined();
  });

  it("rejects an invalid name before it reaches storage", async () => {
    await expect(createSection({ name: "   " })).rejects.toThrow();
    expect(await listSections()).toHaveLength(0);
  });

  it("archiving a section hides its students and schedules too", async () => {
    const sectionId = await createSection({ name: "Grade 11 - Rizal" });
    await createStudent({
      sectionId,
      studentNumber: "2024-1001",
      lastName: "Dela Cruz",
      firstName: "Juan",
      middleName: "",
      gender: "male",
    });
    await createSchedule({
      sectionId,
      title: "Assembly",
      venue: "",
      present: { start: "07:00", end: "07:30" },
      late: { start: "07:30", end: "08:00" },
    });

    await archiveSection(sectionId);

    expect(await listSections()).toHaveLength(0);
    expect(await listStudentsBySection(sectionId)).toHaveLength(0);
    expect(await listSchedulesBySection(sectionId)).toHaveLength(0);
    // The rows survive; only their visibility changed.
    expect(await database.students.count()).toBe(1);
  });
});

describe("students", () => {
  let sectionId: string;

  beforeEach(async () => {
    sectionId = await createSection({ name: "Grade 11 - Rizal" });
  });

  function student(overrides: Record<string, unknown> = {}) {
    return {
      sectionId,
      studentNumber: "2024-1001",
      lastName: "Dela Cruz",
      firstName: "Juan",
      middleName: "Ramos",
      gender: "male",
      ...overrides,
    };
  }

  it("refuses a duplicate student number", async () => {
    await createStudent(student());
    await expect(createStudent(student({ lastName: "Santos" }))).rejects.toBeInstanceOf(
      DuplicateStudentNumberError,
    );
  });

  it("frees a student number once its holder is archived", async () => {
    const id = await createStudent(student());
    await archiveStudent(id);

    await expect(createStudent(student({ lastName: "Santos" }))).resolves.toEqual(expect.any(String));
  });

  it("does not resolve an archived student from a scan", async () => {
    const id = await createStudent(student());
    expect(await findActiveByStudentNumber("2024-1001")).toBeDefined();

    await archiveStudent(id);
    expect(await findActiveByStudentNumber("2024-1001")).toBeUndefined();
  });

  it("searches by name and by number", async () => {
    await createStudent(student());
    await createStudent(student({ studentNumber: "2024-1002", lastName: "Santos", firstName: "Maria" }));

    const all = await listStudentsBySection(sectionId);
    expect(searchStudents(all, "santos")).toHaveLength(1);
    expect(searchStudents(all, "1001")).toHaveLength(1);
    expect(searchStudents(all, "")).toHaveLength(2);
    expect(searchStudents(all, "nobody")).toHaveLength(0);
  });
});

describe("schedules", () => {
  it("lists by start time, and hides archived ones", async () => {
    const sectionId = await createSection({ name: "Grade 11 - Rizal" });

    const late = await createSchedule({
      sectionId,
      title: "Afternoon",
      venue: "",
      present: { start: "13:00", end: "13:30" },
      late: { start: "13:30", end: "14:00" },
    });
    await createSchedule({
      sectionId,
      title: "Assembly",
      venue: "",
      present: { start: "07:00", end: "07:30" },
      late: { start: "07:30", end: "08:00" },
    });

    expect((await listSchedulesBySection(sectionId)).map((s) => s.title)).toEqual([
      "Assembly",
      "Afternoon",
    ]);

    await archiveSchedule(late);
    expect((await listSchedulesBySection(sectionId)).map((s) => s.title)).toEqual(["Assembly"]);
  });
});

describe("records", () => {
  const record = {
    scheduleId: IDS.schedule,
    sectionId: IDS.section,
    studentNumber: "2024-1001",
    date: "2024-03-15",
    status: "present" as const,
    scheduleTitle: "Assembly",
    recordedAt: "2024-03-15T07:10:00.000Z",
  };

  it("enforces one record per student, per schedule, per day", async () => {
    expect(await appendRecords([record])).toBe(1);
    // The unique index rejects the duplicate; the call reports it rather than throwing, because
    // a racing second scan is normal operation, not an error the operator can act on.
    expect(await appendRecords([{ ...record, status: "late" }])).toBe(0);
    expect(await database.records.count()).toBe(1);
  });

  it("counts partial success when only some rows collide", async () => {
    await appendRecords([record]);

    const written = await appendRecords([
      { ...record, status: "late" },
      { ...record, studentNumber: "2024-1002" },
    ]);

    expect(written).toBe(1);
    expect(await database.records.count()).toBe(2);
  });

  it("allows the same student on a different day or a different schedule", async () => {
    await appendRecords([
      record,
      { ...record, date: "2024-03-16" },
      { ...record, scheduleId: IDS.scheduleB },
    ]);
    expect(await database.records.count()).toBe(3);
  });

  it("queries by section and date, and by range inclusively", async () => {
    await appendRecords([
      record,
      { ...record, date: "2024-03-16" },
      { ...record, date: "2024-03-20" },
      { ...record, sectionId: IDS.sectionB, date: "2024-03-16" },
    ]);

    expect(await listRecordsBySectionAndDate(IDS.section, "2024-03-16")).toHaveLength(1);
    expect(await listRecordsBySectionBetween(IDS.section, "2024-03-15", "2024-03-16")).toHaveLength(2);
    expect(await listRecordsBySectionBetween(IDS.section, "2024-03-15", "2024-03-20")).toHaveLength(3);
  });

  it("builds the duplicate-check key set for a date", async () => {
    await appendRecords([record]);
    const keys = await existingKeysForDate("2024-03-15");
    expect(keys.has(`2024-1001|${IDS.schedule}|2024-03-15`)).toBe(true);
  });
});

describe("school days", () => {
  it("records each date once", async () => {
    await markSchoolDay("2024-03-15");
    await markSchoolDay("2024-03-15");
    await markSchoolDay("2024-03-16");

    expect((await listSchoolDays()).map((d) => d.date)).toEqual(["2024-03-15", "2024-03-16"]);
  });
});
