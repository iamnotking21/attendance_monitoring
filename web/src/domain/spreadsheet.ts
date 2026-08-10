/**
 * Spreadsheet-safe cell rendering.
 *
 * Excel and Sheets treat a cell beginning with `=`, `+`, `-`, `@`, or a leading tab/carriage
 * return as a formula. A student named `=cmd|'/c calc'!A1` therefore becomes executable content
 * the moment a coordinator opens the export — a real, repeatedly-exploited attack class (CSV
 * injection). Every user-controlled value that reaches an export goes through here first.
 */

const FORMULA_TRIGGERS = /^[=+\-@\t\r]/;

export function escapeSpreadsheetCell(value: string): string {
  return FORMULA_TRIGGERS.test(value) ? `'${value}` : value;
}

/** RFC 4180 quoting, applied on top of formula neutralisation. */
export function toCsvCell(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return "";
  if (typeof value === "number") return Number.isFinite(value) ? String(value) : "";

  const safe = escapeSpreadsheetCell(value);
  return /[",\r\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
}

export function toCsv(rows: readonly (readonly (string | number | null | undefined)[])[]): string {
  return rows.map((row) => row.map(toCsvCell).join(",")).join("\r\n");
}

/**
 * Strips path separators and reserved characters so a section name can safely become part of a
 * download filename on any platform.
 */
export function toSafeFilename(value: string, fallback = "export"): string {
  const cleaned = value
    .replace(/[\p{Cc}\p{Cf}]/gu, "")
    .replace(/[/\\?%*:|"<>.]/g, "-")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 80);
  return cleaned.length > 0 ? cleaned : fallback;
}
