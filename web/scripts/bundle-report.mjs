import { readdirSync, statSync } from "node:fs";
import path from "node:path";

/**
 * Reports what the browser actually downloads, grouped by route entry and by chunk.
 * Turbopack does not print the per-route table Webpack used to, and "it feels fast" is not a
 * measurement — this is the number the performance budget is checked against.
 */

const CHUNKS = ".next/static/chunks";

function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    return entry.isDirectory() ? walk(full) : [full];
  });
}

const files = walk(CHUNKS)
  .filter((file) => file.endsWith(".js"))
  .map((file) => ({
    name: file.split(path.sep).slice(3).join("/"),
    kb: statSync(file).size / 1024,
  }))
  .sort((a, b) => b.kb - a.kb);

const total = files.reduce((sum, file) => sum + file.kb, 0);

console.log(`${files.length} JavaScript chunks, ${(total / 1024).toFixed(2)} MB uncompressed\n`);
console.log("Largest chunks:");
for (const file of files.slice(0, 15)) {
  console.log(`${file.kb.toFixed(1).padStart(9)} kB  ${file.name}`);
}

const groups = new Map();
for (const file of files) {
  const key = file.name.startsWith("app/") ? file.name.split("/").slice(0, 2).join("/") : "shared";
  groups.set(key, (groups.get(key) ?? 0) + file.kb);
}

console.log("\nBy group:");
for (const [group, kb] of [...groups].sort((a, b) => b[1] - a[1])) {
  console.log(`${kb.toFixed(1).padStart(9)} kB  ${group}`);
}
