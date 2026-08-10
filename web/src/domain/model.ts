import { z } from "zod";

import {
  displayText,
  idSchema,
  isoDateSchema,
  optionalDisplayText,
  studentNumberSchema,
  time24Schema,
} from "./primitives";

/**
 * An entity that has not been stored yet, and so has no primary key. Storage assigns the `id`;
 * keeping the two shapes distinct means a value that was never written can never be mistaken
 * for one that was.
 */
export type New<T extends { id: number }> = Omit<T, "id">;

export const GENDERS = ["male", "female"] as const;
export const genderSchema = z.enum(GENDERS);
export type Gender = z.infer<typeof genderSchema>;

export const ATTENDANCE_STATUSES = ["present", "late", "absent"] as const;
export const attendanceStatusSchema = z.enum(ATTENDANCE_STATUSES);
export type AttendanceStatus = z.infer<typeof attendanceStatusSchema>;

/* ------------------------------------------------------------------ Section */

export const sectionSchema = z.object({
  id: idSchema,
  name: displayText("Section name", 80),
  archived: z.boolean().default(false),
  createdAt: z.string(),
});
export type Section = z.infer<typeof sectionSchema>;

export const sectionInputSchema = sectionSchema.pick({ name: true });
export type SectionInput = z.input<typeof sectionInputSchema>;

/* ------------------------------------------------------------------ Student */

export const studentSchema = z.object({
  id: idSchema,
  sectionId: idSchema,
  studentNumber: studentNumberSchema,
  lastName: displayText("Last name", 60),
  firstName: displayText("First name", 60),
  middleName: optionalDisplayText("Middle name", 60),
  gender: genderSchema,
  archived: z.boolean().default(false),
  createdAt: z.string(),
});
export type Student = z.infer<typeof studentSchema>;

export const studentInputSchema = studentSchema.pick({
  sectionId: true,
  studentNumber: true,
  lastName: true,
  firstName: true,
  middleName: true,
  gender: true,
});
export type StudentInput = z.input<typeof studentInputSchema>;

/** "Dela Cruz, Juan P." — the ordering the printed class list uses. */
export function fullName(student: Pick<Student, "lastName" | "firstName" | "middleName">) {
  const middleInitial = student.middleName ? ` ${student.middleName.charAt(0)}.` : "";
  return `${student.lastName}, ${student.firstName}${middleInitial}`;
}

/* ----------------------------------------------------------------- Schedule */

export const timeWindowSchema = z
  .object({ start: time24Schema, end: time24Schema })
  .refine((w) => w.start < w.end, {
    message: "End time must be after start time",
    path: ["end"],
  });
export type TimeWindow = z.infer<typeof timeWindowSchema>;

export const scheduleSchema = z.object({
  id: idSchema,
  sectionId: idSchema,
  title: displayText("Title", 80),
  venue: optionalDisplayText("Venue", 80),
  present: timeWindowSchema,
  late: timeWindowSchema,
  archived: z.boolean().default(false),
  createdAt: z.string(),
});
export type Schedule = z.infer<typeof scheduleSchema>;

export const scheduleInputSchema = scheduleSchema
  .pick({ sectionId: true, title: true, venue: true, present: true, late: true })
  .refine((s) => s.late.start >= s.present.end, {
    message: "The late window must start when the present window ends, or after it",
    path: ["late", "start"],
  });
export type ScheduleInput = z.input<typeof scheduleInputSchema>;

/* -------------------------------------------------------- Attendance record */

export const attendanceRecordSchema = z.object({
  id: idSchema,
  scheduleId: idSchema,
  sectionId: idSchema,
  studentNumber: studentNumberSchema,
  /** Local calendar date the record belongs to. */
  date: isoDateSchema,
  status: attendanceStatusSchema,
  /** Denormalised so a report stays truthful after a schedule is renamed or archived. */
  scheduleTitle: z.string(),
  recordedAt: z.string(),
});
export type AttendanceRecord = z.infer<typeof attendanceRecordSchema>;
export type NewAttendanceRecord = New<AttendanceRecord>;

/** Uniqueness key: one record per student, per schedule, per day. */
export function recordKey(
  record: Pick<AttendanceRecord, "studentNumber" | "scheduleId" | "date">,
): string {
  return `${record.studentNumber}|${record.scheduleId}|${record.date}`;
}

/* ---------------------------------------------------------------- SchoolDay */

/**
 * Every local date the app has been opened on. This is the denominator for attendance rate —
 * a date nobody ever opened the app is not a school day and must not count against a student.
 */
export const schoolDaySchema = z.object({
  date: isoDateSchema,
  firstSeenAt: z.string(),
});
export type SchoolDay = z.infer<typeof schoolDaySchema>;
