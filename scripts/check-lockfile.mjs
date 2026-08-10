import { readFileSync } from "node:fs";

/**
 * Fails if the lockfile was generated on a platform other than Linux.
 *
 * npm records only the native bindings for the platform it resolved on. A lockfile written on
 * Windows or macOS names no `@tailwindcss/oxide-linux-*`, `lightningcss-linux-*`, or
 * `@next/swc-linux-*` package, so `npm ci` in the Docker image builds a tree that cannot compile
 * CSS and the build dies with "Cannot find native binding".
 *
 * That has now happened twice — once from `npm dedupe`, once from a routine `npm install` on
 * Windows — so it is a check rather than a paragraph in a document nobody reads.
 */

const REQUIRED = [
  "@tailwindcss/oxide-linux-x64-gnu",
  "@tailwindcss/oxide-linux-x64-musl",
  "lightningcss-linux-x64-gnu",
  "lightningcss-linux-x64-musl",
  "@next/swc-linux-x64-gnu",
  "@next/swc-linux-x64-musl",
];

const lock = JSON.parse(readFileSync(new URL("../package-lock.json", import.meta.url), "utf8"));
const present = new Set(
  Object.keys(lock.packages).map((path) => path.split("node_modules/").pop()),
);

const missing = REQUIRED.filter((name) => !present.has(name));

if (missing.length > 0) {
  console.error("package-lock.json is missing Linux native bindings:\n");
  for (const name of missing) console.error(`  - ${name}`);
  console.error(
    "\nThe Docker build will fail with 'Cannot find native binding'. Regenerate the lockfile on" +
      "\nLinux, from the manifests alone:\n" +
      "\n  npm run lockfile:linux\n" +
      "\nSee docs/development.md for why copying the manifests into an empty directory matters.",
  );
  process.exit(1);
}

console.log(`package-lock.json carries all ${REQUIRED.length} required Linux bindings.`);
