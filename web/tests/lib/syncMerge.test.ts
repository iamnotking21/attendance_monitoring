import { emptyChangeSet, incomingWins } from "@attendance/sync/protocol";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import type { AttendanceDatabase } from "@/lib/db";
import { applyChanges } from "@/lib/sync/engine";
import {
  clearConnection,
  readConnection,
  readCursor,
  readPushWatermark,
  writeConnection,
  writeCursor,
} from "@/lib/sync/state";

import { IDS, makeSchedule, makeSection, makeStudent } from "../fixtures";
import { closeDatabase, freshDatabase } from "../helpers/db";

let database: AttendanceDatabase;

beforeEach(async () => {
  database = await freshDatabase();
});

afterEach(async () => {
  await closeDatabase(database);
});

const OLD = "2024-03-15T08:00:00.000Z";
const NEW = "2024-03-15T09:00:00.000Z";

describe("incomingWins", () => {
  it("prefers the later write", () => {
    expect(incomingWins({ updatedAt: NEW, id: "a" }, { updatedAt: OLD, id: "b" })).toBe(true);
    expect(incomingWins({ updatedAt: OLD, id: "a" }, { updatedAt: NEW, id: "b" })).toBe(false);
  });

  it("breaks an exact tie by id, and does so symmetrically", () => {
    // Two devices must reach the same answer without talking to each other, so the rule has to
    // be total and antisymmetric.
    const a = { updatedAt: NEW, id: "aaa" };
    const b = { updatedAt: NEW, id: "bbb" };

    expect(incomingWins(b, a)).toBe(true);
    expect(incomingWins(a, b)).toBe(false);
  });

  it("never lets a row win against itself", () => {
    const row = { updatedAt: NEW, id: "aaa" };
    expect(incomingWins(row, row)).toBe(false);
  });
});

describe("applyChanges", () => {
  it("inserts rows this device has never seen", async () => {
    const applied = await applyChanges({
      ...emptyChangeSet(),
      sections: [makeSection()],
      schedules: [makeSchedule()],
      students: [makeStudent()],
    });

    expect(applied).toBe(3);
    expect((await database.sections.get(IDS.section))?.name).toBe("Grade 11 - Rizal");
  });

  it("applies a remote edit that is newer than the local row", async () => {
    await database.sections.put(makeSection({ name: "Local", updatedAt: OLD }));

    await applyChanges({
      ...emptyChangeSet(),
      sections: [makeSection({ name: "Remote", updatedAt: NEW })],
    });

    expect((await database.sections.get(IDS.section))?.name).toBe("Remote");
  });

  it("discards a remote edit that is older than the local row", async () => {
    // The device that was offline all morning finally syncs and offers a stale name. Accepting
    // it would silently undo an edit someone already made and saw take effect.
    await database.sections.put(makeSection({ name: "Local and newer", updatedAt: NEW }));

    const applied = await applyChanges({
      ...emptyChangeSet(),
      sections: [makeSection({ name: "Remote but older", updatedAt: OLD })],
    });

    expect(applied).toBe(0);
    expect((await database.sections.get(IDS.section))?.name).toBe("Local and newer");
  });

  it("replicates an archive like any other field", async () => {
    await database.sections.put(makeSection({ updatedAt: OLD }));

    await applyChanges({
      ...emptyChangeSet(),
      sections: [makeSection({ archived: true, updatedAt: NEW })],
    });

    expect((await database.sections.get(IDS.section))?.archived).toBe(true);
  });

  it("does not add a record this device already has under a different id", async () => {
    // Two devices scanned the same badge offline, so each minted its own record id. The natural
    // key is what recognises them as the same event.
    await database.records.put({
      id: IDS.record,
      scheduleId: IDS.schedule,
      sectionId: IDS.section,
      studentNumber: "2024-1001",
      date: "2024-03-15",
      status: "present",
      scheduleTitle: "Morning Assembly",
      recordedAt: OLD,
    });

    const applied = await applyChanges({
      ...emptyChangeSet(),
      records: [
        {
          id: "99999999-9999-4999-8999-999999999999",
          scheduleId: IDS.schedule,
          sectionId: IDS.section,
          studentNumber: "2024-1001",
          date: "2024-03-15",
          status: "late",
          scheduleTitle: "Morning Assembly",
          recordedAt: NEW,
        },
      ],
    });

    expect(applied).toBe(0);
    expect(await database.records.count()).toBe(1);
    expect((await database.records.get(IDS.record))?.status).toBe("present");
  });

  it("adds a record for the same student on a different day", async () => {
    await database.records.put({
      id: IDS.record,
      scheduleId: IDS.schedule,
      sectionId: IDS.section,
      studentNumber: "2024-1001",
      date: "2024-03-15",
      status: "present",
      scheduleTitle: "Morning Assembly",
      recordedAt: OLD,
    });

    const applied = await applyChanges({
      ...emptyChangeSet(),
      records: [
        {
          id: "88888888-8888-4888-8888-888888888888",
          scheduleId: IDS.schedule,
          sectionId: IDS.section,
          studentNumber: "2024-1001",
          date: "2024-03-16",
          status: "present",
          scheduleTitle: "Morning Assembly",
          recordedAt: NEW,
        },
      ],
    });

    expect(applied).toBe(1);
    expect(await database.records.count()).toBe(2);
  });

  it("is idempotent: applying the same change set twice changes nothing the second time", async () => {
    const changes = {
      ...emptyChangeSet(),
      sections: [makeSection()],
      students: [makeStudent()],
      schoolDays: [{ date: "2024-03-15", firstSeenAt: OLD }],
    };

    expect(await applyChanges(changes)).toBe(3);
    expect(await applyChanges(changes)).toBe(0);
    expect(await database.sections.count()).toBe(1);
  });

  it("keeps the first record of a school day rather than overwriting it", async () => {
    await applyChanges({
      ...emptyChangeSet(),
      schoolDays: [{ date: "2024-03-15", firstSeenAt: OLD }],
    });
    await applyChanges({
      ...emptyChangeSet(),
      schoolDays: [{ date: "2024-03-15", firstSeenAt: NEW }],
    });

    expect((await database.schoolDays.get("2024-03-15"))?.firstSeenAt).toBe(OLD);
  });
});

describe("sync state", () => {
  it("round-trips the connection", async () => {
    await writeConnection({
      workspaceId: IDS.section,
      workspaceName: "Test School",
      token: "a-token",
      joinCode: "ABCD-EFGH-JKLM",
      connectedAt: OLD,
    });

    expect((await readConnection())?.workspaceName).toBe("Test School");
  });

  it("starts from the beginning of the change log", async () => {
    expect(await readCursor()).toBe(0);
    expect(await readPushWatermark()).toBe("1970-01-01T00:00:00.000Z");
  });

  it("disconnecting forgets the workspace but keeps every record", async () => {
    // Disconnecting is not a delete. Losing a term of attendance because someone tapped the
    // wrong button would be indefensible.
    await database.sections.put(makeSection());
    await writeConnection({
      workspaceId: IDS.section,
      workspaceName: "Test School",
      token: "a-token",
      connectedAt: OLD,
    });
    await writeCursor(42);

    await clearConnection();

    expect(await readConnection()).toBeUndefined();
    expect(await readCursor()).toBe(0);
    expect(await database.sections.count()).toBe(1);
  });
});
