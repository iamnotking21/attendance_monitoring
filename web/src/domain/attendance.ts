import type { AttendanceStatus, NewAttendanceRecord, Schedule } from "./model";
import { recordKey } from "./model";
import type { Id, IsoDate } from "./primitives";
import { timeToMinutes } from "./time";

/**
 * The daily lifecycle of one schedule.
 *
 *   before ──▶ present ──▶ [gap] ──▶ late ──▶ closed
 *
 * `gap` exists only when a schedule leaves a deliberate pause between the two windows; when the
 * late window starts exactly as the present window ends, the state machine never enters it.
 * A scan is graded by whichever window is open. Scans in `before`, `gap`, or `closed` record
 * nothing at all — matching the original app, where a scan outside both windows was discarded.
 */
export type ScheduleWindowState = "before" | "present" | "gap" | "late" | "closed";

export function windowStateAt(schedule: Schedule, atMinutes: number): ScheduleWindowState {
  const presentStart = timeToMinutes(schedule.present.start);
  const presentEnd = timeToMinutes(schedule.present.end);
  const lateStart = timeToMinutes(schedule.late.start);
  const lateEnd = timeToMinutes(schedule.late.end);

  if (atMinutes < presentStart) return "before";
  if (atMinutes < presentEnd) return "present";
  if (atMinutes < lateStart) return "gap";
  if (atMinutes < lateEnd) return "late";
  return "closed";
}

/** The status a scan earns in a given state, or null when the scan records nothing. */
export function statusForWindow(state: ScheduleWindowState): AttendanceStatus | null {
  if (state === "present") return "present";
  if (state === "late") return "late";
  return null;
}

/**
 * Minute at which this schedule next changes state, or null once it is closed for the day.
 * The UI uses this to wake up exactly when the late window shuts and run the absentee sweep,
 * instead of polling.
 */
export function nextTransitionMinute(schedule: Schedule, atMinutes: number): number | null {
  const boundaries = [
    timeToMinutes(schedule.present.start),
    timeToMinutes(schedule.present.end),
    timeToMinutes(schedule.late.start),
    timeToMinutes(schedule.late.end),
  ].sort((a, b) => a - b);

  for (const boundary of boundaries) {
    if (boundary > atMinutes) return boundary;
  }
  return null;
}

export function isCollectingScans(schedule: Schedule, atMinutes: number): boolean {
  return statusForWindow(windowStateAt(schedule, atMinutes)) !== null;
}

/* --------------------------------------------------------------- Scan intake */

export interface ScanContext {
  studentNumber: string;
  /** Active schedules belonging to the scanned student's section. */
  schedules: readonly Schedule[];
  /** Keys of records that already exist, from {@link recordKey}. */
  existingKeys: ReadonlySet<string>;
  date: IsoDate;
  atMinutes: number;
  recordedAt: string;
}

export interface ScanResolution {
  /** Records to append. Empty when nothing about this scan was new. */
  created: NewAttendanceRecord[];
  /** Schedules the student was already recorded for today — a second scan changes nothing. */
  duplicateScheduleIds: Id[];
  /** Schedules whose windows were shut at scan time. */
  inactiveScheduleIds: Id[];
}

/**
 * Grades one scan against every active schedule for the student's section.
 *
 * A student may sit in more than one schedule at once (a class and a flag ceremony, say), so a
 * single scan can produce several records. Duplicate suppression is per schedule, not per scan:
 * scanning again after a new window opens correctly records the new window while leaving the
 * already-recorded one untouched.
 */
export function resolveScan(context: ScanContext): ScanResolution {
  const resolution: ScanResolution = {
    created: [],
    duplicateScheduleIds: [],
    inactiveScheduleIds: [],
  };

  for (const schedule of context.schedules) {
    if (schedule.id === undefined || schedule.archived) continue;

    const status = statusForWindow(windowStateAt(schedule, context.atMinutes));
    if (status === null) {
      resolution.inactiveScheduleIds.push(schedule.id);
      continue;
    }

    const key = recordKey({
      studentNumber: context.studentNumber,
      scheduleId: schedule.id,
      date: context.date,
    });
    if (context.existingKeys.has(key)) {
      resolution.duplicateScheduleIds.push(schedule.id);
      continue;
    }

    resolution.created.push({
      scheduleId: schedule.id,
      sectionId: schedule.sectionId,
      studentNumber: context.studentNumber,
      date: context.date,
      status,
      scheduleTitle: schedule.title,
      recordedAt: context.recordedAt,
    });
  }

  return resolution;
}

/* ------------------------------------------------------------ Absentee sweep */

export interface SweepContext {
  schedule: Schedule;
  /** Student numbers of every active student in the schedule's section. */
  activeStudentNumbers: readonly string[];
  existingKeys: ReadonlySet<string>;
  date: IsoDate;
  atMinutes: number;
  recordedAt: string;
}

/**
 * Once the late window has shut, every active student in the section with no record for this
 * schedule and date is absent.
 *
 * Idempotent by construction: the records it writes are exactly the ones whose absence it
 * detects, so a second run finds nothing left to write. Returns [] while the schedule is still
 * open — sweeping early would mark students absent who have not had their chance to scan.
 */
export function absentRecordsFor(context: SweepContext): NewAttendanceRecord[] {
  const { schedule } = context;
  if (schedule.id === undefined || schedule.archived) return [];
  if (windowStateAt(schedule, context.atMinutes) !== "closed") return [];

  const scheduleId = schedule.id;

  return context.activeStudentNumbers
    .filter(
      (studentNumber) =>
        !context.existingKeys.has(recordKey({ studentNumber, scheduleId, date: context.date })),
    )
    .map((studentNumber) => ({
      scheduleId,
      sectionId: schedule.sectionId,
      studentNumber,
      date: context.date,
      status: "absent" as const,
      scheduleTitle: schedule.title,
      recordedAt: context.recordedAt,
    }));
}
