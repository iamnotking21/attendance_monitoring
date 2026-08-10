import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

import postgres from "postgres";

/**
 * Applies the SQL files in `migrations/` in filename order, once each.
 *
 * Hand-written SQL rather than generated migrations: the schema depends on a shared sequence and
 * two unique indexes whose exact shape is the whole point, and a generator that quietly reorders
 * or rewrites them would be a liability rather than a convenience.
 */
const directory = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "migrations");

async function main(): Promise<void> {
  const url = process.env.DATABASE_URL;
  if (!url) {
    console.error("DATABASE_URL is not set. Point it at a Postgres database and retry.");
    process.exitCode = 1;
    return;
  }

  const sql = postgres(url, { max: 1, prepare: false });

  try {
    await sql`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        name text PRIMARY KEY,
        applied_at timestamptz NOT NULL DEFAULT now()
      )
    `;

    const applied = new Set(
      (await sql<{ name: string }[]>`SELECT name FROM schema_migrations`).map((r) => r.name),
    );

    const files = (await readdir(directory)).filter((file) => file.endsWith(".sql")).sort();

    for (const file of files) {
      if (applied.has(file)) {
        console.log(`· ${file} (already applied)`);
        continue;
      }

      const statements = await readFile(path.join(directory, file), "utf8");
      // One transaction per file, so a failure leaves the schema at the last good migration
      // rather than half-applied.
      await sql.begin(async (tx) => {
        await tx.unsafe(statements);
        await tx`INSERT INTO schema_migrations (name) VALUES (${file})`;
      });
      console.log(`✓ ${file}`);
    }

    console.log(`Schema up to date (${files.length} migration${files.length === 1 ? "" : "s"}).`);
  } finally {
    await sql.end({ timeout: 5 });
  }
}

await main();
