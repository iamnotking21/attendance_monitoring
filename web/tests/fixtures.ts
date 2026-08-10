import type { Gender, Schedule, Section, Student } from "@/domain/model";

const CREATED_AT = "2024-03-01T00:00:00.000Z";

export function makeSection(overrides: Partial<Section> = {}): Section {
  return { id: 1, name: "Grade 11 - Rizal", archived: false, createdAt: CREATED_AT, ...overrides };
}

export function makeStudent(overrides: Partial<Student> = {}): Student {
  return {
    id: 1,
    sectionId: 1,
    studentNumber: "2024-1001",
    lastName: "Dela Cruz",
    firstName: "Juan",
    middleName: "Ramos",
    gender: "male" as Gender,
    archived: false,
    createdAt: CREATED_AT,
    ...overrides,
  };
}

/** Present 07:00–07:30, late 07:30–08:00 — the shape the legacy app shipped with. */
export function makeSchedule(overrides: Partial<Schedule> = {}): Schedule {
  return {
    id: 1,
    sectionId: 1,
    title: "Morning Assembly",
    venue: "Quadrangle",
    present: { start: "07:00", end: "07:30" },
    late: { start: "07:30", end: "08:00" },
    archived: false,
    createdAt: CREATED_AT,
    ...overrides,
  };
}

export const AT = {
  before: 6 * 60 + 59,
  presentStart: 7 * 60,
  presentMiddle: 7 * 60 + 15,
  presentEnd: 7 * 60 + 30,
  lateMiddle: 7 * 60 + 45,
  lateEnd: 8 * 60,
  after: 8 * 60 + 1,
} as const;
