import { drizzle, type PostgresJsDatabase } from "drizzle-orm/postgres-js";
import postgres from "postgres";

import * as schema from "./schema";

export type Database = PostgresJsDatabase<typeof schema>;

let cached: { sql: postgres.Sql; db: Database } | null = null;

export class DatabaseNotConfiguredError extends Error {
  constructor() {
    super(
      "Sync is not configured on this deployment: DATABASE_URL is not set. The app still works offline.",
    );
    this.name = "DatabaseNotConfiguredError";
  }
}

export function isSyncConfigured(): boolean {
  return Boolean(process.env.DATABASE_URL);
}

/**
 * One pooled connection per warm serverless instance.
 *
 * `max: 1` because a serverless function handles one request at a time; a larger pool multiplied
 * by every warm instance is how a small app exhausts a free-tier connection limit. `prepare:
 * false` is required by transaction-mode poolers such as pgBouncer and Neon's pooled endpoint,
 * which cannot carry prepared statements across checkouts.
 */
export function db(): Database {
  const url = process.env.DATABASE_URL;
  if (!url) throw new DatabaseNotConfiguredError();

  if (!cached) {
    const sql = postgres(url, {
      max: 1,
      idle_timeout: 20,
      connect_timeout: 10,
      prepare: false,
    });
    cached = { sql, db: drizzle(sql, { schema }) };
  }

  return cached.db;
}

/** Test seam: point the module at a database created by the test harness. */
export function __setDatabaseForTests(next: { sql: postgres.Sql; db: Database } | null): void {
  cached = next;
}

export async function closeDatabase(): Promise<void> {
  await cached?.sql.end({ timeout: 5 });
  cached = null;
}
