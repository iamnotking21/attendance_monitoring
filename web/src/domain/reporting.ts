import type { AttendanceRecord, AttendanceStatus, Gender, Student } from "./model";
import { fullName } from "./model";
import type { IsoDate } from "./primitives";
import { isWithinRange } from "./time";

export interface StatusTally {
  present: number;
  late: number;
  absent: number;
}

export const EMPTY_TALLY: Readonly<StatusTally> = Object.freeze({
  present: 0,
  late: 0,
  absent: 0,
});

export function tally(records: readonly AttendanceRecord[]): StatusTally {
  const result: StatusTally = { present: 0, late: 0, absent: 0 };
  for (const record of records) {
    result[record.status] += 1;
  }
  return result;
}

export function totalOf(counts: StatusTally): number {
  return counts.present + counts.late + counts.absent;
}

/**
 * Share of sessions the student actually turned up for. Late still counts as attending — it is
 * a punctuality problem, not an absence, and conflating the two was a real reporting bug in the
 * original app.
 */
export function attendanceRate(counts: StatusTally): number {
  const total = totalOf(counts);
  return total === 0 ? 0 : (counts.present + counts.late) / total;
}

export function formatRate(rate: number): string {
  return `${Math.round(rate * 100)}%`;
}

/* --------------------------------------------------------- Per-student report */

export interface StudentSummary {
  student: Student;
  displayName: string;
  counts: StatusTally;
  sessions: number;
  rate: number;
}

export interface SummaryRange {
  start: IsoDate;
  end: IsoDate;
}

/**
 * Attendance per student over an inclusive date range.
 *
 * Students with no records at all are still listed, with a zero tally — a student who never
 * appears in the data is exactly the one a coordinator needs to see, so dropping them would
 * defeat the report.
 */
export function summariseStudents(
  students: readonly Student[],
  records: readonly AttendanceRecord[],
  range: SummaryRange,
): StudentSummary[] {
  const byStudentNumber = new Map<string, AttendanceRecord[]>();
  for (const record of records) {
    if (!isWithinRange(record.date, range.start, range.end)) continue;
    const bucket = byStudentNumber.get(record.studentNumber);
    if (bucket) bucket.push(record);
    else byStudentNumber.set(record.studentNumber, [record]);
  }

  return students
    .filter((student) => !student.archived)
    .map((student) => {
      const counts = tally(byStudentNumber.get(student.studentNumber) ?? []);
      return {
        student,
        displayName: fullName(student),
        counts,
        sessions: totalOf(counts),
        rate: attendanceRate(counts),
      };
    })
    .sort((a, b) => a.displayName.localeCompare(b.displayName));
}

/* ------------------------------------------------------------ Daily dashboard */

export interface DashboardEntry {
  student: Student;
  displayName: string;
  status: AttendanceStatus;
  scheduleTitle: string;
  recordedAt: string;
}

export interface DashboardBreakdown {
  entries: DashboardEntry[];
  counts: StatusTally;
  /** Active students in the section with no record at all yet for the chosen day. */
  unaccountedFor: Student[];
}

/**
 * The day view: who was present, late, or absent, resolved back to real students.
 *
 * Records whose student has since been deleted are dropped rather than rendered as an unknown
 * row — the record survives in storage for the audit trail, but a roster is a list of people.
 */
export function buildDashboard(
  students: readonly Student[],
  records: readonly AttendanceRecord[],
  date: IsoDate,
): DashboardBreakdown {
  const activeStudents = students.filter((student) => !student.archived);
  const byNumber = new Map(activeStudents.map((student) => [student.studentNumber, student]));

  const forDate = records.filter((record) => record.date === date);
  const entries: DashboardEntry[] = [];
  const accountedFor = new Set<string>();

  for (const record of forDate) {
    const student = byNumber.get(record.studentNumber);
    if (!student) continue;
    accountedFor.add(record.studentNumber);
    entries.push({
      student,
      displayName: fullName(student),
      status: record.status,
      scheduleTitle: record.scheduleTitle,
      recordedAt: record.recordedAt,
    });
  }

  entries.sort((a, b) => a.displayName.localeCompare(b.displayName));

  return {
    entries,
    counts: tally(forDate.filter((record) => byNumber.has(record.studentNumber))),
    unaccountedFor: activeStudents.filter(
      (student) => !accountedFor.has(student.studentNumber),
    ),
  };
}

export function entriesByStatus(
  breakdown: DashboardBreakdown,
  status: AttendanceStatus,
): DashboardEntry[] {
  return breakdown.entries.filter((entry) => entry.status === status);
}

export function entriesByGender(
  entries: readonly DashboardEntry[],
  gender: Gender,
): DashboardEntry[] {
  return entries.filter((entry) => entry.student.gender === gender);
}
