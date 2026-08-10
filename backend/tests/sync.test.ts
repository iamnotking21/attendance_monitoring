import { randomUUID } from "node:crypto";

import { afterAll, beforeEach, describe, expect, it } from "vitest";

import {
  authenticate,
  bearerToken,
  createWorkspace,
  InvalidJoinCodeError,
  joinWorkspace,
  revokeDevice,
  UnauthorizedError,
} from "../src/auth";
import { consumeRateLimit } from "../src/rateLimit";
import { pull, push } from "../src/sync";
import {
  aRecord,
  aSchedule,
  aSection,
  aStudent,
  changes,
  closeTestDatabase,
  resetTables,
  testDatabase,
} from "./helpers";

const db = testDatabase();

beforeEach(async () => {
  await resetTables();
});

afterAll(async () => {
  await closeTestDatabase();
});

async function newWorkspace(name = "Pedro Fernandez NHS") {
  return createWorkspace(name, db);
}

describe("workspace enrolment", () => {
  it("issues a token that authenticates back to the workspace", async () => {
    const created = await newWorkspace();

    const workspace = await authenticate(created.token, db);
    expect(workspace.id).toBe(created.workspace.id);
    expect(workspace.name).toBe("Pedro Fernandez NHS");
  });

  it("rejects an unknown token", async () => {
    await newWorkspace();
    await expect(authenticate("not-a-real-token", db)).rejects.toBeInstanceOf(UnauthorizedError);
    await expect(authenticate(null, db)).rejects.toBeInstanceOf(UnauthorizedError);
  });

  it("gives a joining device its own token for the same workspace", async () => {
    const created = await newWorkspace();

    const joined = await joinWorkspace(created.joinCode, db);

    expect(joined.workspace.id).toBe(created.workspace.id);
    // A distinct token is what makes per-device revocation possible.
    expect(joined.token).not.toBe(created.token);

    expect((await authenticate(joined.token, db)).id).toBe(created.workspace.id);
    expect((await authenticate(created.token, db)).id).toBe(created.workspace.id);
  });

  it("forgives the formatting of a hand-typed join code", async () => {
    const created = await newWorkspace();
    const messy = ` ${created.joinCode.toLowerCase().replace(/-/g, "")} `;

    await expect(joinWorkspace(messy, db)).resolves.toBeDefined();
  });

  it("rejects a wrong join code", async () => {
    await newWorkspace();
    await expect(joinWorkspace("AAAA-BBBB-CCCC", db)).rejects.toBeInstanceOf(
      InvalidJoinCodeError,
    );
  });

  it("revoking one device leaves the others working", async () => {
    const created = await newWorkspace();
    const joined = await joinWorkspace(created.joinCode, db);

    await revokeDevice(joined.token, db);

    await expect(authenticate(joined.token, db)).rejects.toBeInstanceOf(UnauthorizedError);
    await expect(authenticate(created.token, db)).resolves.toBeDefined();
  });

  it("parses the Authorization header", () => {
    expect(bearerToken("Bearer abc123")).toBe("abc123");
    expect(bearerToken("bearer abc123")).toBe("abc123");
    expect(bearerToken("Basic abc123")).toBeNull();
    expect(bearerToken(null)).toBeNull();
  });
});

describe("push and pull", () => {
  it("returns everything a device pushed", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    const schedule = aSchedule(section.id);
    const student = aStudent(section.id);
    const record = aRecord(schedule.id, section.id);

    const pushed = await push(
      workspace.id,
      changes({
        sections: [section],
        schedules: [schedule],
        students: [student],
        records: [record],
        schoolDays: [{ date: "2024-03-15", firstSeenAt: "2024-03-15T07:00:00.000Z" }],
      }),
      db,
    );
    expect(pushed.applied).toBe(5);

    const pulled = await pull(workspace.id, 0, 500, db);
    expect(pulled.changes.sections).toHaveLength(1);
    expect(pulled.changes.schedules[0].present).toEqual({ start: "07:00", end: "07:30" });
    expect(pulled.changes.students[0].studentNumber).toBe("2024-1001");
    expect(pulled.changes.records[0].status).toBe("present");
    expect(pulled.changes.schoolDays[0].date).toBe("2024-03-15");
    expect(pulled.hasMore).toBe(false);
  });

  it("a second pull at the returned cursor sees nothing new", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    await push(workspace.id, changes({ sections: [section] }), db);

    const first = await pull(workspace.id, 0, 500, db);
    const second = await pull(workspace.id, first.cursor, 500, db);

    expect(second.changes.sections).toHaveLength(0);
    expect(second.cursor).toBe(first.cursor);
  });

  it("an edit after a pull shows up at the next pull", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    await push(workspace.id, changes({ sections: [section] }), db);
    const first = await pull(workspace.id, 0, 500, db);

    await push(
      workspace.id,
      changes({
        sections: [{ ...section, name: "Grade 11 - Bonifacio", updatedAt: "2024-03-16T08:00:00.000Z" }],
      }),
      db,
    );

    const second = await pull(workspace.id, first.cursor, 500, db);
    expect(second.changes.sections).toHaveLength(1);
    expect(second.changes.sections[0].name).toBe("Grade 11 - Bonifacio");
    expect(second.cursor).toBeGreaterThan(first.cursor);
  });

  it("paginates without dropping rows", async () => {
    const { workspace } = await newWorkspace();
    const sections = Array.from({ length: 12 }, (_, index) =>
      aSection({ name: `Section ${String(index).padStart(2, "0")}` }),
    );
    await push(workspace.id, changes({ sections }), db);

    const seen: string[] = [];
    let cursor = 0;
    let guard = 0;

    for (;;) {
      const page = await pull(workspace.id, cursor, 5, db);
      seen.push(...page.changes.sections.map((row) => row.name));
      cursor = page.cursor;
      if (!page.hasMore) break;
      if ((guard += 1) > 10) throw new Error("pagination did not terminate");
    }

    expect(seen.sort()).toEqual(sections.map((s) => s.name).sort());
  });

  it("keeps one workspace's rows out of another's pull", async () => {
    const a = await newWorkspace("School A");
    const b = await newWorkspace("School B");

    await push(a.workspace.id, changes({ sections: [aSection({ name: "Only in A" })] }), db);

    const pulled = await pull(b.workspace.id, 0, 500, db);
    expect(pulled.changes.sections).toHaveLength(0);
  });
});

describe("conflict resolution", () => {
  it("a newer edit wins", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection({ name: "Original", updatedAt: "2024-03-15T08:00:00.000Z" });
    await push(workspace.id, changes({ sections: [section] }), db);

    await push(
      workspace.id,
      changes({
        sections: [{ ...section, name: "Newer", updatedAt: "2024-03-15T09:00:00.000Z" }],
      }),
      db,
    );

    const pulled = await pull(workspace.id, 0, 500, db);
    expect(pulled.changes.sections[0].name).toBe("Newer");
  });

  it("an older edit arriving late does not overwrite a newer one", async () => {
    // The everyday offline case: a phone that was in a bag all morning finally reconnects and
    // pushes an edit that was already superseded on another device.
    const { workspace } = await newWorkspace();
    const section = aSection({ name: "Newer", updatedAt: "2024-03-15T09:00:00.000Z" });
    await push(workspace.id, changes({ sections: [section] }), db);

    const result = await push(
      workspace.id,
      changes({
        sections: [{ ...section, name: "Stale", updatedAt: "2024-03-15T08:00:00.000Z" }],
      }),
      db,
    );

    expect(result.applied).toBe(0);
    expect(result.skipped).toBe(1);

    const pulled = await pull(workspace.id, 0, 500, db);
    expect(pulled.changes.sections[0].name).toBe("Newer");
  });

  it("breaks an exact timestamp tie the same way every time", async () => {
    const { workspace } = await newWorkspace();
    const sameMoment = "2024-03-15T09:00:00.000Z";

    // Two rows with the same id cannot exist, so the tiebreak is exercised by pushing the same
    // id twice with identical timestamps: the rule says the larger id wins, and since the ids
    // are equal here neither push may overwrite the other.
    const section = aSection({ name: "First", updatedAt: sameMoment });
    await push(workspace.id, changes({ sections: [section] }), db);

    const result = await push(
      workspace.id,
      changes({ sections: [{ ...section, name: "Second", updatedAt: sameMoment }] }),
      db,
    );

    expect(result.applied).toBe(0);
    const pulled = await pull(workspace.id, 0, 500, db);
    expect(pulled.changes.sections[0].name).toBe("First");
  });

  it("archiving replicates like any other edit", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    await push(workspace.id, changes({ sections: [section] }), db);

    await push(
      workspace.id,
      changes({
        sections: [{ ...section, archived: true, updatedAt: "2024-03-16T08:00:00.000Z" }],
      }),
      db,
    );

    const pulled = await pull(workspace.id, 0, 500, db);
    expect(pulled.changes.sections[0].archived).toBe(true);
  });
});

describe("attendance record deduplication", () => {
  it("two devices recording the same student produce one record", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    const schedule = aSchedule(section.id);
    await push(workspace.id, changes({ sections: [section], schedules: [schedule] }), db);

    // Same student, same schedule, same day — but each device minted its own id offline.
    const fromPhone = aRecord(schedule.id, section.id, { id: randomUUID() });
    const fromTablet = aRecord(schedule.id, section.id, { id: randomUUID(), status: "late" });

    await push(workspace.id, changes({ records: [fromPhone] }), db);
    const second = await push(workspace.id, changes({ records: [fromTablet] }), db);

    expect(second.applied).toBe(0);

    const pulled = await pull(workspace.id, 0, 500, db);
    expect(pulled.changes.records).toHaveLength(1);
    // First writer wins: attendance is append-only, so the earlier record stands.
    expect(pulled.changes.records[0].status).toBe("present");
  });

  it("the same record pushed twice after a failed response is not duplicated", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    const schedule = aSchedule(section.id);
    await push(workspace.id, changes({ sections: [section], schedules: [schedule] }), db);

    const record = aRecord(schedule.id, section.id);
    await push(workspace.id, changes({ records: [record] }), db);
    const retry = await push(workspace.id, changes({ records: [record] }), db);

    expect(retry.applied).toBe(0);
    expect((await pull(workspace.id, 0, 500, db)).changes.records).toHaveLength(1);
  });

  it("allows the same student on another day and another schedule", async () => {
    const { workspace } = await newWorkspace();
    const section = aSection();
    const schedule = aSchedule(section.id);
    const other = aSchedule(section.id, { title: "Homeroom" });
    await push(
      workspace.id,
      changes({ sections: [section], schedules: [schedule, other] }),
      db,
    );

    const result = await push(
      workspace.id,
      changes({
        records: [
          aRecord(schedule.id, section.id),
          aRecord(schedule.id, section.id, { date: "2024-03-16" }),
          aRecord(other.id, section.id),
        ],
      }),
      db,
    );

    expect(result.applied).toBe(3);
  });
});

describe("rate limiting", () => {
  it("allows up to the limit, then refuses", async () => {
    const key = `test:${randomUUID()}`;

    for (let attempt = 1; attempt <= 3; attempt += 1) {
      const result = await consumeRateLimit(key, 3, 60, db);
      expect(result.allowed).toBe(true);
    }

    const blocked = await consumeRateLimit(key, 3, 60, db);
    expect(blocked.allowed).toBe(false);
    expect(blocked.remaining).toBe(0);
    expect(blocked.retryAfterSeconds).toBeGreaterThan(0);
  });

  it("counts each caller separately", async () => {
    const first = `test:${randomUUID()}`;
    const second = `test:${randomUUID()}`;

    await consumeRateLimit(first, 1, 60, db);
    expect((await consumeRateLimit(first, 1, 60, db)).allowed).toBe(false);
    expect((await consumeRateLimit(second, 1, 60, db)).allowed).toBe(true);
  });
});
