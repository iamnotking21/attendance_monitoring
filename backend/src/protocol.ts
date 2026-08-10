import { z } from "zod";

/**
 * The wire contract between a device and the sync server.
 *
 * Defined here rather than imported from the web app's `domain/` on purpose. The local model is
 * free to change shape whenever the app wants; the wire format cannot, because a phone that has
 * not been updated in a month still has to sync. Keeping them separate makes that independence
 * explicit instead of accidental.
 */

export const PROTOCOL_VERSION = 1 as const;

/* ------------------------------------------------------------------- shared */

const uuid = z.uuid();
const timestamp = z.iso.datetime();
const isoDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");
const time24 = z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Time must be HH:mm");
const studentNumber = z
  .string()
  .min(1)
  .max(32)
  .regex(/^[A-Za-z0-9][A-Za-z0-9._-]*$/);
const shortText = (max: number) => z.string().max(max);

/* ------------------------------------------------------------------ entities */

export const wireSectionSchema = z.object({
  id: uuid,
  name: shortText(80).min(1),
  archived: z.boolean(),
  createdAt: timestamp,
  updatedAt: timestamp,
});

export const wireStudentSchema = z.object({
  id: uuid,
  sectionId: uuid,
  studentNumber,
  lastName: shortText(60).min(1),
  firstName: shortText(60).min(1),
  middleName: shortText(60),
  gender: z.enum(["male", "female"]),
  archived: z.boolean(),
  createdAt: timestamp,
  updatedAt: timestamp,
});

export const wireScheduleSchema = z.object({
  id: uuid,
  sectionId: uuid,
  title: shortText(80).min(1),
  venue: shortText(80),
  present: z.object({ start: time24, end: time24 }),
  late: z.object({ start: time24, end: time24 }),
  archived: z.boolean(),
  createdAt: timestamp,
  updatedAt: timestamp,
});

export const wireRecordSchema = z.object({
  id: uuid,
  scheduleId: uuid,
  sectionId: uuid,
  studentNumber,
  date: isoDate,
  status: z.enum(["present", "late", "absent"]),
  scheduleTitle: shortText(80),
  recordedAt: timestamp,
});

export const wireSchoolDaySchema = z.object({
  date: isoDate,
  firstSeenAt: timestamp,
});

export type WireSection = z.infer<typeof wireSectionSchema>;
export type WireStudent = z.infer<typeof wireStudentSchema>;
export type WireSchedule = z.infer<typeof wireScheduleSchema>;
export type WireRecord = z.infer<typeof wireRecordSchema>;
export type WireSchoolDay = z.infer<typeof wireSchoolDaySchema>;

/* ------------------------------------------------------------------- batches */

/**
 * A push is capped well below anything a real school produces in one batch. Without a ceiling,
 * a single request could ask the server to validate and upsert an unbounded number of rows.
 */
const MAX_PER_PUSH = 2_000;

export const changeSetSchema = z.object({
  sections: z.array(wireSectionSchema).max(MAX_PER_PUSH).default([]),
  students: z.array(wireStudentSchema).max(MAX_PER_PUSH).default([]),
  schedules: z.array(wireScheduleSchema).max(MAX_PER_PUSH).default([]),
  records: z.array(wireRecordSchema).max(MAX_PER_PUSH).default([]),
  schoolDays: z.array(wireSchoolDaySchema).max(MAX_PER_PUSH).default([]),
});

export type ChangeSet = z.infer<typeof changeSetSchema>;

export function emptyChangeSet(): ChangeSet {
  return { sections: [], students: [], schedules: [], records: [], schoolDays: [] };
}

export function changeSetSize(changes: ChangeSet): number {
  return (
    changes.sections.length +
    changes.students.length +
    changes.schedules.length +
    changes.records.length +
    changes.schoolDays.length
  );
}

/* ------------------------------------------------------------------ requests */

export const createWorkspaceRequestSchema = z.object({
  name: shortText(80).min(1),
});

export const joinWorkspaceRequestSchema = z.object({
  joinCode: z.string().min(8).max(32),
});

export const workspaceCredentialsSchema = z.object({
  workspaceId: uuid,
  name: z.string(),
  /** Shown once, so another device can join. Never returned again. */
  joinCode: z.string().optional(),
  token: z.string(),
});

export type WorkspaceCredentials = z.infer<typeof workspaceCredentialsSchema>;

/**
 * The cursor is a server-assigned sequence number, not a timestamp.
 *
 * Device clocks disagree — sometimes by minutes, occasionally by years. A timestamp cursor
 * silently skips rows written while the clock was wrong. A sequence assigned by the database is
 * monotonic by construction, so "everything after N" means exactly that.
 */
export const pullRequestSchema = z.object({
  since: z.number().int().min(0).default(0),
  limit: z.number().int().min(1).max(1000).default(500),
});

export const pullResponseSchema = z.object({
  changes: changeSetSchema,
  cursor: z.number().int().min(0),
  hasMore: z.boolean(),
  serverTime: timestamp,
});

export const pushRequestSchema = z.object({
  changes: changeSetSchema,
});

export const pushResponseSchema = z.object({
  applied: z.number().int().min(0),
  skipped: z.number().int().min(0),
  cursor: z.number().int().min(0),
  serverTime: timestamp,
});

export type PullRequest = z.infer<typeof pullRequestSchema>;
export type PullResponse = z.infer<typeof pullResponseSchema>;
export type PushRequest = z.infer<typeof pushRequestSchema>;
export type PushResponse = z.infer<typeof pushResponseSchema>;

/* -------------------------------------------------------------------- errors */

export const errorResponseSchema = z.object({
  error: z.string(),
  code: z.enum([
    "unauthorized",
    "not_found",
    "invalid_request",
    "rate_limited",
    "unavailable",
  ]),
});

export type ErrorResponse = z.infer<typeof errorResponseSchema>;

/**
 * Last-write-wins, with a deterministic tiebreak.
 *
 * Two devices editing the same row while both offline is normal, not exceptional. Whoever wrote
 * last wins; if the timestamps are identical — clocks are coarse, and a bulk edit can produce
 * the same millisecond — the larger id wins. Arbitrary, but identical on every device and on the
 * server, which is the only property that matters. Without it, replicas can disagree forever.
 */
export function incomingWins(
  incoming: { updatedAt: string; id: string },
  existing: { updatedAt: string; id: string },
): boolean {
  if (incoming.updatedAt !== existing.updatedAt) {
    return incoming.updatedAt > existing.updatedAt;
  }
  return incoming.id > existing.id;
}
