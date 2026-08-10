import { clearMeta, readMeta, writeMeta } from "@/lib/db";

/**
 * The device's own sync bookkeeping. Never replicated — this is about *this* machine: which
 * workspace it belongs to, and how far through the server's change log it has read.
 */

const KEY = {
  connection: "sync:connection",
  cursor: "sync:cursor",
  lastSyncedAt: "sync:lastSyncedAt",
  pushWatermark: "sync:pushWatermark",
} as const;

export interface SyncConnection {
  workspaceId: string;
  workspaceName: string;
  /** Bearer token for this device. Held locally only; the server stores a hash. */
  token: string;
  /** Shown once after creating a workspace so another device can join. */
  joinCode?: string;
  connectedAt: string;
}

export async function readConnection(): Promise<SyncConnection | undefined> {
  return readMeta<SyncConnection>(KEY.connection);
}

export async function writeConnection(connection: SyncConnection): Promise<void> {
  await writeMeta(KEY.connection, connection);
}

/**
 * Forgets the workspace but keeps every local record.
 *
 * Disconnecting is not a delete. The attendance already taken on this device is the school's
 * data, and losing it because someone tapped "disconnect" would be indefensible.
 */
export async function clearConnection(): Promise<void> {
  await Promise.all([
    clearMeta(KEY.connection),
    clearMeta(KEY.cursor),
    clearMeta(KEY.lastSyncedAt),
    clearMeta(KEY.pushWatermark),
  ]);
}

export async function readCursor(): Promise<number> {
  return (await readMeta<number>(KEY.cursor)) ?? 0;
}

export async function writeCursor(cursor: number): Promise<void> {
  await writeMeta(KEY.cursor, cursor);
}

export async function readLastSyncedAt(): Promise<string | undefined> {
  return readMeta<string>(KEY.lastSyncedAt);
}

export async function writeLastSyncedAt(at: string): Promise<void> {
  await writeMeta(KEY.lastSyncedAt, at);
}

/**
 * Everything edited after this instant still needs sending.
 *
 * A watermark rather than a per-row dirty flag or an outbox table: every entity already carries
 * `updatedAt`, so "what has changed since the last push" is a query rather than extra
 * bookkeeping that could drift out of step with the data it describes.
 *
 * The cost is a little re-sending — a row pulled from another device can fall after the
 * watermark and get pushed straight back. The server recognises it as identical and applies
 * nothing, so the effect is a few wasted bytes rather than a wrong result.
 */
export async function readPushWatermark(): Promise<string> {
  return (await readMeta<string>(KEY.pushWatermark)) ?? "1970-01-01T00:00:00.000Z";
}

export async function writePushWatermark(at: string): Promise<void> {
  await writeMeta(KEY.pushWatermark, at);
}
