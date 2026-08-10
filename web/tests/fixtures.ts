import type { Gender, Schedule, Section, Student } from "@/domain/model";

const CREATED_AT = "2024-03-01T00:00:00.000Z";

/**
 * Fixed UUIDs rather than generated ones, so a failing assertion prints a stable identifier and
 * two runs of the same test compare equal.
 */
export const IDS = {
  section: "11111111-1111-4111-8111-111111111111",
  sectionB: "11111111-1111-4111-8111-222222222222",
  schedule: "22222222-2222-4222-8222-111111111111",
  scheduleB: "22222222-2222-4222-8222-222222222222",
  student: "33333333-3333-4333-8333-111111111111",
  studentB: "33333333-3333-4333-8333-222222222222",
  studentC: "33333333-3333-4333-8333-333333333333",
  record: "44444444-4444-4444-8444-111111111111",
} as const;

export function makeSection(overrides: Partial<Section> = {}): Section {
  return {
    id: IDS.section,
    name: "Grade 11 - Rizal",
    archived: false,
    createdAt: CREATED_AT,
    updatedAt: CREATED_AT,
    ...overrides,
  };
}

export function makeStudent(overrides: Partial<Student> = {}): Student {
  return {
    id: IDS.student,
    sectionId: IDS.section,
    studentNumber: "2024-1001",
    lastName: "Dela Cruz",
    firstName: "Juan",
    middleName: "Ramos",
    gender: "male" as Gender,
    archived: false,
    createdAt: CREATED_AT,
    updatedAt: CREATED_AT,
    ...overrides,
  };
}

/** Present 07:00–07:30, late 07:30–08:00 — the shape the legacy app shipped with. */
export function makeSchedule(overrides: Partial<Schedule> = {}): Schedule {
  return {
    id: IDS.schedule,
    sectionId: IDS.section,
    title: "Morning Assembly",
    venue: "Quadrangle",
    present: { start: "07:00", end: "07:30" },
    late: { start: "07:30", end: "08:00" },
    archived: false,
    createdAt: CREATED_AT,
    updatedAt: CREATED_AT,
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
