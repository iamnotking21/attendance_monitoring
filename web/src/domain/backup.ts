import { z } from "zod";

import {
  attendanceRecordSchema,
  scheduleSchema,
  schoolDaySchema,
  sectionSchema,
  studentSchema,
} from "./model";

export const BACKUP_FORMAT = "attendance-monitoring-backup" as const;
export const BACKUP_VERSION = 1 as const;

/**
 * Upper bounds on an imported file. A backup is user-supplied input, and without a ceiling a
 * crafted file could pin the main thread while the browser tries to validate a million rows.
 * These sit far above any realistic school: 500 sections, 50k students, 2M records.
 */
const LIMITS = {
  sections: 500,
  students: 50_000,
  schedules: 5_000,
  records: 2_000_000,
  schoolDays: 20_000,
} as const;

export const backupSchema = z.object({
  format: z.literal(BACKUP_FORMAT),
  version: z.literal(BACKUP_VERSION),
  exportedAt: z.string(),
  data: z.object({
    sections: z.array(sectionSchema).max(LIMITS.sections),
    students: z.array(studentSchema).max(LIMITS.students),
    schedules: z.array(scheduleSchema).max(LIMITS.schedules),
    records: z.array(attendanceRecordSchema).max(LIMITS.records),
    schoolDays: z.array(schoolDaySchema).max(LIMITS.schoolDays),
  }),
});

export type Backup = z.infer<typeof backupSchema>;
export type BackupData = Backup["data"];

export function createBackup(data: BackupData, exportedAt: string): Backup {
  return { format: BACKUP_FORMAT, version: BACKUP_VERSION, exportedAt, data };
}

export type BackupParseResult =
  | { ok: true; backup: Backup }
  | { ok: false; error: string };

/**
 * Validates an uploaded backup before a single row of it reaches storage.
 *
 * Everything is rejected by default: unknown formats, future versions, and rows that fail the
 * same entity schemas the app enforces on its own writes. Restoring an unvalidated file would
 * let a hand-edited backup smuggle arbitrary shapes straight into the database.
 */
export function parseBackup(raw: unknown): BackupParseResult {
  const parsed = backupSchema.safeParse(raw);
  if (parsed.success) {
    return { ok: true, backup: parsed.data };
  }

  const shallow = z
    .object({ format: z.string().optional(), version: z.number().optional() })
    .safeParse(raw);

  if (shallow.success && shallow.data.format !== undefined) {
    if (shallow.data.format !== BACKUP_FORMAT) {
      return { ok: false, error: "That file is not an Attendance Monitoring backup." };
    }
    if (shallow.data.version !== undefined && shallow.data.version > BACKUP_VERSION) {
      return {
        ok: false,
        error: `This backup was written by a newer version of the app (v${shallow.data.version}). Update the app, then restore it.`,
      };
    }
  }

  const first = parsed.error.issues[0];
  const where = first?.path.length ? ` at ${first.path.join(".")}` : "";
  return {
    ok: false,
    error: `The backup file is not valid${where}: ${first?.message ?? "unrecognised structure"}`,
  };
}

export function parseBackupJson(text: string): BackupParseResult {
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    return { ok: false, error: "That file is not valid JSON." };
  }
  return parseBackup(raw);
}
