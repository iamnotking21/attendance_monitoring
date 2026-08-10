import { and, asc, eq, gt, sql } from "drizzle-orm";

import { db, type Database } from "./client";
import {
  changeSetSize,
  emptyChangeSet,
  type ChangeSet,
  type PullResponse,
  type PushResponse,
} from "./protocol";
import { records, schedules, schoolDays, sections, students } from "./schema";

/** Advances the shared cursor so every other device sees the row on its next pull. */
const nextSeq = sql`nextval('sync_seq')`;

/**
 * Replication in two directions.
 *
 * The model is deliberately plain: last-write-wins per row, keyed by the writer's `updatedAt`,
 * with the row id as a deterministic tiebreak. It is not a CRDT and does not merge field by
 * field — if two teachers rename the same section while both offline, one name wins whole. For
 * a roster edited by a handful of people that is the honest trade: an operation log would double
 * the storage and the complexity to resolve a conflict that is rare and cheap to correct by hand.
 *
 * Attendance records, which are the high-volume table, never conflict at all: they are
 * append-only and deduplicated by (student, schedule, date).
 */

/* --------------------------------------------------------------------- pull */

interface Cursored {
  serverSeq: number;
}

export async function pull(
  workspaceId: string,
  since: number,
  limit: number,
  database: Database = db(),
): Promise<PullResponse> {
  const [sectionRows, studentRows, scheduleRows, recordRows, dayRows] = await Promise.all([
    database
      .select()
      .from(sections)
      .where(and(eq(sections.workspaceId, workspaceId), gt(sections.serverSeq, since)))
      .orderBy(asc(sections.serverSeq))
      .limit(limit + 1),
    database
      .select()
      .from(students)
      .where(and(eq(students.workspaceId, workspaceId), gt(students.serverSeq, since)))
      .orderBy(asc(students.serverSeq))
      .limit(limit + 1),
    database
      .select()
      .from(schedules)
      .where(and(eq(schedules.workspaceId, workspaceId), gt(schedules.serverSeq, since)))
      .orderBy(asc(schedules.serverSeq))
      .limit(limit + 1),
    database
      .select()
      .from(records)
      .where(and(eq(records.workspaceId, workspaceId), gt(records.serverSeq, since)))
      .orderBy(asc(records.serverSeq))
      .limit(limit + 1),
    database
      .select()
      .from(schoolDays)
      .where(and(eq(schoolDays.workspaceId, workspaceId), gt(schoolDays.serverSeq, since)))
      .orderBy(asc(schoolDays.serverSeq))
      .limit(limit + 1),
  ]);

  // Each table is read independently, then the five streams are merged and cut at `limit`.
  //
  // Each query asks for one row more than the page size. Without that extra row a page filled
  // entirely from one table looks identical to the end of the data, and every client stops
  // paginating after the first page — which is exactly what the pagination test caught.
  //
  // The cut is safe: every table returned its first `limit + 1` rows above the cursor in
  // sequence order, so nothing below the resulting cursor can have been left behind.
  const everything: Cursored[] = [
    ...sectionRows,
    ...studentRows,
    ...scheduleRows,
    ...recordRows,
    ...dayRows,
  ].sort((a, b) => a.serverSeq - b.serverSeq);

  const included = everything.slice(0, limit);
  const cursor = included.length > 0 ? included[included.length - 1].serverSeq : since;
  const hasMore = everything.length > included.length;

  const within = (row: Cursored) => row.serverSeq <= cursor;

  const changes: ChangeSet = {
    sections: sectionRows.filter(within).map((row) => ({
      id: row.id,
      name: row.name,
      archived: row.archived,
      createdAt: row.createdAt.toISOString(),
      updatedAt: row.updatedAt.toISOString(),
    })),
    students: studentRows.filter(within).map((row) => ({
      id: row.id,
      sectionId: row.sectionId,
      studentNumber: row.studentNumber,
      lastName: row.lastName,
      firstName: row.firstName,
      middleName: row.middleName,
      gender: row.gender === "female" ? "female" : "male",
      archived: row.archived,
      createdAt: row.createdAt.toISOString(),
      updatedAt: row.updatedAt.toISOString(),
    })),
    schedules: scheduleRows.filter(within).map((row) => ({
      id: row.id,
      sectionId: row.sectionId,
      title: row.title,
      venue: row.venue,
      present: row.present,
      late: row.late,
      archived: row.archived,
      createdAt: row.createdAt.toISOString(),
      updatedAt: row.updatedAt.toISOString(),
    })),
    records: recordRows.filter(within).map((row) => ({
      id: row.id,
      scheduleId: row.scheduleId,
      sectionId: row.sectionId,
      studentNumber: row.studentNumber,
      date: row.date,
      status: row.status as "present" | "late" | "absent",
      scheduleTitle: row.scheduleTitle,
      recordedAt: row.recordedAt.toISOString(),
    })),
    schoolDays: dayRows.filter(within).map((row) => ({
      date: row.date,
      firstSeenAt: row.firstSeenAt.toISOString(),
    })),
  };

  return { changes, cursor, hasMore, serverTime: new Date().toISOString() };
}

/* --------------------------------------------------------------------- push */

export async function push(
  workspaceId: string,
  changes: ChangeSet,
  database: Database = db(),
): Promise<PushResponse> {
  const total = changeSetSize(changes);
  if (total === 0) {
    return {
      applied: 0,
      skipped: 0,
      cursor: await currentCursor(workspaceId, database),
      serverTime: new Date().toISOString(),
    };
  }

  let applied = 0;
  // The highest sequence this push wrote. Returning it from the upserts means the response
  // cursor comes free, instead of costing five more index scans in a separate round trip.
  let highestSeq = 0;

  const track = (rows: { serverSeq: number }[]) => {
    applied += rows.length;
    for (const row of rows) if (row.serverSeq > highestSeq) highestSeq = row.serverSeq;
  };

  await database.transaction(async (tx) => {
    if (changes.sections.length > 0) {
      const written = await tx
        .insert(sections)
        .values(
          changes.sections.map((row) => ({
            workspaceId,
            id: row.id,
            name: row.name,
            archived: row.archived,
            createdAt: new Date(row.createdAt),
            updatedAt: new Date(row.updatedAt),
          })),
        )
        .onConflictDoUpdate({
          target: [sections.workspaceId, sections.id],
          set: {
            name: sql`excluded.name`,
            archived: sql`excluded.archived`,
            updatedAt: sql`excluded.updated_at`,
            serverSeq: nextSeq,
          },
          // The stale half of a conflict is discarded here rather than overwriting a newer edit.
          setWhere: sql`excluded.updated_at > ${sections.updatedAt}
            OR (excluded.updated_at = ${sections.updatedAt} AND excluded.id > ${sections.id})`,
        })
        .returning({ serverSeq: sections.serverSeq });
      track(written);
    }

    if (changes.schedules.length > 0) {
      const written = await tx
        .insert(schedules)
        .values(
          changes.schedules.map((row) => ({
            workspaceId,
            id: row.id,
            sectionId: row.sectionId,
            title: row.title,
            venue: row.venue,
            present: row.present,
            late: row.late,
            archived: row.archived,
            createdAt: new Date(row.createdAt),
            updatedAt: new Date(row.updatedAt),
          })),
        )
        .onConflictDoUpdate({
          target: [schedules.workspaceId, schedules.id],
          set: {
            sectionId: sql`excluded.section_id`,
            title: sql`excluded.title`,
            venue: sql`excluded.venue`,
            present: sql`excluded.present`,
            late: sql`excluded.late`,
            archived: sql`excluded.archived`,
            updatedAt: sql`excluded.updated_at`,
            serverSeq: nextSeq,
          },
          setWhere: sql`excluded.updated_at > ${schedules.updatedAt}
            OR (excluded.updated_at = ${schedules.updatedAt} AND excluded.id > ${schedules.id})`,
        })
        .returning({ serverSeq: schedules.serverSeq });
      track(written);
    }

    if (changes.students.length > 0) {
      const written = await tx
        .insert(students)
        .values(
          changes.students.map((row) => ({
            workspaceId,
            id: row.id,
            sectionId: row.sectionId,
            studentNumber: row.studentNumber,
            lastName: row.lastName,
            firstName: row.firstName,
            middleName: row.middleName,
            gender: row.gender,
            archived: row.archived,
            createdAt: new Date(row.createdAt),
            updatedAt: new Date(row.updatedAt),
          })),
        )
        .onConflictDoUpdate({
          target: [students.workspaceId, students.id],
          set: {
            sectionId: sql`excluded.section_id`,
            studentNumber: sql`excluded.student_number`,
            lastName: sql`excluded.last_name`,
            firstName: sql`excluded.first_name`,
            middleName: sql`excluded.middle_name`,
            gender: sql`excluded.gender`,
            archived: sql`excluded.archived`,
            updatedAt: sql`excluded.updated_at`,
            serverSeq: nextSeq,
          },
          setWhere: sql`excluded.updated_at > ${students.updatedAt}
            OR (excluded.updated_at = ${students.updatedAt} AND excluded.id > ${students.id})`,
        })
        .returning({ serverSeq: students.serverSeq });
      track(written);
    }

    if (changes.records.length > 0) {
      // Append-only, so there is nothing to merge. Both unique indexes are covered by the
      // untargeted DO NOTHING: the natural key catches two devices recording the same student,
      // and the primary key catches the same row being pushed twice after a failed response.
      const written = await tx
        .insert(records)
        .values(
          changes.records.map((row) => ({
            workspaceId,
            id: row.id,
            scheduleId: row.scheduleId,
            sectionId: row.sectionId,
            studentNumber: row.studentNumber,
            date: row.date,
            status: row.status,
            scheduleTitle: row.scheduleTitle,
            recordedAt: new Date(row.recordedAt),
          })),
        )
        .onConflictDoNothing()
        .returning({ serverSeq: records.serverSeq });
      track(written);
    }

    if (changes.schoolDays.length > 0) {
      const written = await tx
        .insert(schoolDays)
        .values(
          changes.schoolDays.map((row) => ({
            workspaceId,
            date: row.date,
            firstSeenAt: new Date(row.firstSeenAt),
          })),
        )
        .onConflictDoNothing()
        .returning({ serverSeq: schoolDays.serverSeq });
      track(written);
    }
  });

  return {
    applied,
    skipped: total - applied,
    // Only fall back to querying when this push wrote nothing, which is the cheap case anyway.
    cursor: highestSeq > 0 ? highestSeq : await currentCursor(workspaceId, database),
    serverTime: new Date().toISOString(),
  };
}

/**
 * The highest sequence this workspace has reached.
 *
 * Returned after a push so the device can advance its cursor past its own writes instead of
 * pulling them straight back.
 */
export async function currentCursor(
  workspaceId: string,
  database: Database = db(),
): Promise<number> {
  const [row] = await database.execute<{ cursor: number | null }>(sql`
    SELECT GREATEST(
      COALESCE((SELECT MAX(server_seq) FROM sections WHERE workspace_id = ${workspaceId}), 0),
      COALESCE((SELECT MAX(server_seq) FROM students WHERE workspace_id = ${workspaceId}), 0),
      COALESCE((SELECT MAX(server_seq) FROM schedules WHERE workspace_id = ${workspaceId}), 0),
      COALESCE((SELECT MAX(server_seq) FROM attendance_records WHERE workspace_id = ${workspaceId}), 0),
      COALESCE((SELECT MAX(server_seq) FROM school_days WHERE workspace_id = ${workspaceId}), 0)
    ) AS cursor
  `);
  return Number(row?.cursor ?? 0);
}

export { emptyChangeSet };
