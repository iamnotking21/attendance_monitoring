import { sql } from "drizzle-orm";

import { db, type Database } from "./client";

export interface RateLimitResult {
  allowed: boolean;
  remaining: number;
  retryAfterSeconds: number;
}

/**
 * Fixed-window rate limiting, held in Postgres.
 *
 * An in-process counter would be useless here: serverless instances do not share memory, so each
 * cold start would begin counting from zero and the limit would bound nothing. The window is
 * advanced and the counter incremented in a single statement, which keeps two concurrent
 * requests from both reading a stale count.
 *
 * Applied to workspace creation and joining — the endpoints where an attacker gets something for
 * free by repeating them. Sync itself is authenticated and bounded by the change-set cap.
 */
export async function consumeRateLimit(
  key: string,
  limit: number,
  windowSeconds: number,
  database: Database = db(),
): Promise<RateLimitResult> {
  const rows = await database.execute<{ count: number; window_start: Date }>(sql`
    INSERT INTO rate_limits (key, window_start, count)
    VALUES (${key}, now(), 1)
    ON CONFLICT (key) DO UPDATE SET
      window_start = CASE
        WHEN rate_limits.window_start < now() - ${sql.raw(`interval '${windowSeconds} seconds'`)}
        THEN now() ELSE rate_limits.window_start END,
      count = CASE
        WHEN rate_limits.window_start < now() - ${sql.raw(`interval '${windowSeconds} seconds'`)}
        THEN 1 ELSE rate_limits.count + 1 END
    RETURNING count, window_start
  `);

  const row = Array.isArray(rows) ? rows[0] : undefined;
  const count = Number(row?.count ?? 1);
  const windowStart = row?.window_start ? new Date(row.window_start) : new Date();
  const elapsed = (Date.now() - windowStart.getTime()) / 1000;

  return {
    allowed: count <= limit,
    remaining: Math.max(0, limit - count),
    retryAfterSeconds: Math.max(1, Math.ceil(windowSeconds - elapsed)),
  };
}

/**
 * Identifies the caller for rate-limiting purposes.
 *
 * Deliberately hashed before use so a raw IP address is never written to the database — the
 * limiter needs to tell callers apart, not to know who they are.
 */
export function clientKey(prefix: string, forwardedFor: string | null): string {
  const ip = forwardedFor?.split(",")[0]?.trim() ?? "unknown";
  return `${prefix}:${ip}`;
}
