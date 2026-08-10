import { createHash, randomBytes, timingSafeEqual } from "node:crypto";

import { eq, sql } from "drizzle-orm";

import { db, type Database } from "./client";
import { deviceTokens, workspaces } from "./schema";

/**
 * A workspace is one school's data, and the unit of access.
 *
 * There is deliberately no account system. Attendance is taken by whoever is holding the phone
 * at the gate, and forcing a login on that person buys nothing while guaranteeing a shared
 * password on a sticky note. Instead: creating a workspace mints a short join code, and any
 * device that presents the code receives its own long-lived token.
 *
 * Tokens are per device, not per workspace, so a lost phone can be revoked on its own.
 */

/**
 * Excludes I, L, O, U, 0, and 1. A join code gets read aloud and typed in a hurry, and those are
 * the characters people get wrong.
 */
const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
const CODE_LENGTH = 12;

export function generateToken(): string {
  return randomBytes(32).toString("base64url");
}

export function generateJoinCode(): string {
  const bytes = randomBytes(CODE_LENGTH);
  let code = "";
  for (let index = 0; index < CODE_LENGTH; index += 1) {
    // Modulo bias is negligible here: 256 % 30 skews the first 16 letters by under 0.4%, which
    // is immaterial against 30^12 possibilities.
    code += CODE_ALPHABET[bytes[index] % CODE_ALPHABET.length];
    if (index === 3 || index === 7) code += "-";
  }
  return code;
}

/**
 * SHA-256 rather than a password hash.
 *
 * Argon2 and bcrypt exist to slow down guessing of low-entropy human passwords. These are 256-bit
 * random values and a 12-character code from a 30-letter alphabet — roughly 59 bits — so there is
 * nothing to guess offline, and a deliberately slow hash on every sync request would cost real
 * latency for no security. What matters is that plaintext is never stored, so a leaked database
 * yields no usable credential.
 */
export function hashSecret(secret: string): string {
  return createHash("sha256").update(normalise(secret)).digest("hex");
}

/** Join codes are typed by hand, so case, spaces, and the grouping hyphens are all forgiven. */
function normalise(value: string): string {
  return value.trim().replace(/[\s-]/g, "").toUpperCase();
}

/** Constant-time compare, so a response cannot be timed to recover a secret byte by byte. */
function secretsMatch(a: string, b: string): boolean {
  const left = Buffer.from(a, "utf8");
  const right = Buffer.from(b, "utf8");
  if (left.length !== right.length) return false;
  return timingSafeEqual(left, right);
}

export interface Workspace {
  id: string;
  name: string;
}

export interface EnrolledDevice {
  workspace: Workspace;
  token: string;
}

export async function createWorkspace(
  name: string,
  database: Database = db(),
): Promise<EnrolledDevice & { joinCode: string }> {
  const joinCode = generateJoinCode();
  const token = generateToken();

  return database.transaction(async (tx) => {
    const [row] = await tx
      .insert(workspaces)
      .values({ name, joinCodeHash: hashSecret(joinCode) })
      .returning({ id: workspaces.id, name: workspaces.name });

    await tx.insert(deviceTokens).values({
      tokenHash: hashSecret(token),
      workspaceId: row.id,
    });

    return { workspace: { id: row.id, name: row.name }, token, joinCode };
  });
}

export class InvalidJoinCodeError extends Error {
  constructor() {
    super("That join code does not match any workspace.");
    this.name = "InvalidJoinCodeError";
  }
}

/**
 * Exchanges a join code for a token belonging to this device.
 *
 * The code stays valid so further devices can enrol; it is the workspace's shared secret, and
 * the UI says so. Each device walks away with a distinct token, which is what makes revocation
 * possible later.
 */
export async function joinWorkspace(
  joinCode: string,
  database: Database = db(),
): Promise<EnrolledDevice> {
  const codeHash = hashSecret(joinCode);

  const [row] = await database
    .select()
    .from(workspaces)
    .where(eq(workspaces.joinCodeHash, codeHash))
    .limit(1);

  if (!row || !secretsMatch(row.joinCodeHash, codeHash)) throw new InvalidJoinCodeError();

  const token = generateToken();
  await database
    .insert(deviceTokens)
    .values({ tokenHash: hashSecret(token), workspaceId: row.id });

  return { workspace: { id: row.id, name: row.name }, token };
}

export class UnauthorizedError extends Error {
  constructor() {
    super("That sync token is not valid.");
    this.name = "UnauthorizedError";
  }
}

/** Resolves a bearer token to a workspace, or throws. Every sync request goes through here. */
export async function authenticate(
  token: string | null | undefined,
  database: Database = db(),
): Promise<Workspace> {
  if (!token) throw new UnauthorizedError();

  const tokenHash = hashSecret(token);
  const [row] = await database
    .select({
      id: workspaces.id,
      name: workspaces.name,
      tokenHash: deviceTokens.tokenHash,
    })
    .from(deviceTokens)
    .innerJoin(workspaces, eq(workspaces.id, deviceTokens.workspaceId))
    .where(eq(deviceTokens.tokenHash, tokenHash))
    .limit(1);

  if (!row || !secretsMatch(row.tokenHash, tokenHash)) throw new UnauthorizedError();

  // Best-effort liveness marker for revoking devices that have gone quiet. Deliberately not
  // awaited into the critical path's failure mode: a failed bookkeeping write must not break sync.
  void database
    .update(deviceTokens)
    .set({ lastSeenAt: sql`now()` })
    .where(eq(deviceTokens.tokenHash, tokenHash))
    .catch(() => undefined);

  return { id: row.id, name: row.name };
}

export function bearerToken(header: string | null | undefined): string | null {
  if (!header) return null;
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match ? match[1] : null;
}

/** Cuts off a single device. The workspace and every other device keep working. */
export async function revokeDevice(token: string, database: Database = db()): Promise<void> {
  await database.delete(deviceTokens).where(eq(deviceTokens.tokenHash, hashSecret(token)));
}
