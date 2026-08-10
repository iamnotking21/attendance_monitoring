import { sql } from "drizzle-orm";
import {
  bigint,
  boolean,
  date,
  index,
  integer,
  jsonb,
  pgTable,
  text,
  timestamp,
  uniqueIndex,
  uuid,
} from "drizzle-orm/pg-core";

/**
 * Every row carries a `workspaceId` and a `serverSeq`.
 *
 * `workspaceId` is the tenancy boundary — one school, one workspace — and every query filters on
 * it, so a bug in one query cannot leak another school's roster.
 *
 * `serverSeq` is a database-assigned monotonic counter that drives the pull cursor. It exists
 * because device clocks cannot be trusted: a phone whose clock is a day slow would otherwise
 * write rows that every other device skips over forever.
 */

export const workspaces = pgTable("workspaces", {
  id: uuid("id").primaryKey().defaultRandom(),
  name: text("name").notNull(),
  /** SHA-256 of the join code. The code itself is shown once and never stored. */
  joinCodeHash: text("join_code_hash").notNull().unique(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

/**
 * One row per device that has joined. Tokens are per device rather than per workspace so that a
 * lost phone can be cut off on its own, instead of forcing every other device to re-enrol.
 */
export const deviceTokens = pgTable("device_tokens", {
  tokenHash: text("token_hash").primaryKey(),
  workspaceId: uuid("workspace_id")
    .notNull()
    .references(() => workspaces.id, { onDelete: "cascade" }),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  lastSeenAt: timestamp("last_seen_at", { withTimezone: true }).notNull().defaultNow(),
});

export const sections = pgTable(
  "sections",
  {
    workspaceId: uuid("workspace_id")
      .notNull()
      .references(() => workspaces.id, { onDelete: "cascade" }),
    id: uuid("id").notNull(),
    name: text("name").notNull(),
    archived: boolean("archived").notNull().default(false),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull(),
    serverSeq: bigint("server_seq", { mode: "number" })
      .notNull()
      .default(sql`nextval('sync_seq')`),
  },
  (table) => [
    uniqueIndex("sections_pk").on(table.workspaceId, table.id),
    index("sections_cursor").on(table.workspaceId, table.serverSeq),
  ],
);

export const students = pgTable(
  "students",
  {
    workspaceId: uuid("workspace_id")
      .notNull()
      .references(() => workspaces.id, { onDelete: "cascade" }),
    id: uuid("id").notNull(),
    sectionId: uuid("section_id").notNull(),
    studentNumber: text("student_number").notNull(),
    lastName: text("last_name").notNull(),
    firstName: text("first_name").notNull(),
    middleName: text("middle_name").notNull().default(""),
    gender: text("gender").notNull(),
    archived: boolean("archived").notNull().default(false),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull(),
    serverSeq: bigint("server_seq", { mode: "number" })
      .notNull()
      .default(sql`nextval('sync_seq')`),
  },
  (table) => [
    uniqueIndex("students_pk").on(table.workspaceId, table.id),
    index("students_cursor").on(table.workspaceId, table.serverSeq),
    index("students_number").on(table.workspaceId, table.studentNumber),
  ],
);

export const schedules = pgTable(
  "schedules",
  {
    workspaceId: uuid("workspace_id")
      .notNull()
      .references(() => workspaces.id, { onDelete: "cascade" }),
    id: uuid("id").notNull(),
    sectionId: uuid("section_id").notNull(),
    title: text("title").notNull(),
    venue: text("venue").notNull().default(""),
    // Stored as JSON because a window is only ever read and written whole; splitting it into
    // four columns would buy no query the app makes.
    present: jsonb("present").$type<{ start: string; end: string }>().notNull(),
    late: jsonb("late").$type<{ start: string; end: string }>().notNull(),
    archived: boolean("archived").notNull().default(false),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull(),
    serverSeq: bigint("server_seq", { mode: "number" })
      .notNull()
      .default(sql`nextval('sync_seq')`),
  },
  (table) => [
    uniqueIndex("schedules_pk").on(table.workspaceId, table.id),
    index("schedules_cursor").on(table.workspaceId, table.serverSeq),
  ],
);

export const records = pgTable(
  "attendance_records",
  {
    workspaceId: uuid("workspace_id")
      .notNull()
      .references(() => workspaces.id, { onDelete: "cascade" }),
    id: uuid("id").notNull(),
    scheduleId: uuid("schedule_id").notNull(),
    sectionId: uuid("section_id").notNull(),
    studentNumber: text("student_number").notNull(),
    date: date("date").notNull(),
    status: text("status").notNull(),
    scheduleTitle: text("schedule_title").notNull().default(""),
    recordedAt: timestamp("recorded_at", { withTimezone: true }).notNull(),
    serverSeq: bigint("server_seq", { mode: "number" })
      .notNull()
      .default(sql`nextval('sync_seq')`),
  },
  (table) => [
    uniqueIndex("records_pk").on(table.workspaceId, table.id),
    // The same invariant the device enforces, restated where two devices meet: one record per
    // student, per schedule, per day. Two phones scanning the same badge offline both arrive
    // here, and this is what stops the second from becoming a duplicate.
    uniqueIndex("records_natural_key").on(
      table.workspaceId,
      table.studentNumber,
      table.scheduleId,
      table.date,
    ),
    index("records_cursor").on(table.workspaceId, table.serverSeq),
  ],
);

export const schoolDays = pgTable(
  "school_days",
  {
    workspaceId: uuid("workspace_id")
      .notNull()
      .references(() => workspaces.id, { onDelete: "cascade" }),
    date: date("date").notNull(),
    firstSeenAt: timestamp("first_seen_at", { withTimezone: true }).notNull(),
    serverSeq: bigint("server_seq", { mode: "number" })
      .notNull()
      .default(sql`nextval('sync_seq')`),
  },
  (table) => [
    uniqueIndex("school_days_pk").on(table.workspaceId, table.date),
    index("school_days_cursor").on(table.workspaceId, table.serverSeq),
  ],
);

/**
 * Fixed-window rate limiting, kept in the database rather than in memory because serverless
 * functions do not share memory — an in-process counter would reset on every cold start and
 * limit nothing.
 */
export const rateLimits = pgTable("rate_limits", {
  key: text("key").primaryKey(),
  windowStart: timestamp("window_start", { withTimezone: true }).notNull(),
  count: integer("count").notNull().default(0),
});
