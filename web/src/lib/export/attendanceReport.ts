import type { StudentSummary, SummaryRange } from "@/domain/reporting";
import { escapeSpreadsheetCell, toCsv, toSafeFilename } from "@/domain/spreadsheet";

const HEADERS = [
  "Student number",
  "Name",
  "Gender",
  "Present",
  "Late",
  "Absent",
  "Sessions",
  "Attendance rate",
] as const;

export interface ReportContext {
  sectionName: string;
  range: SummaryRange;
  summaries: readonly StudentSummary[];
}

export function reportFilename(context: ReportContext, extension: string): string {
  const section = toSafeFilename(context.sectionName, "section");
  return `attendance-${section}-${context.range.start}-to-${context.range.end}.${extension}`;
}

export function buildCsv(context: ReportContext): string {
  const rows: (string | number)[][] = [
    [...HEADERS],
    ...context.summaries.map((summary) => [
      summary.student.studentNumber,
      summary.displayName,
      summary.student.gender === "male" ? "Boy" : "Girl",
      summary.counts.present,
      summary.counts.late,
      summary.counts.absent,
      summary.sessions,
      `${Math.round(summary.rate * 100)}%`,
    ]),
  ];
  return toCsv(rows);
}

/** A BOM makes Excel open the file as UTF-8 instead of mangling every accented name. */
export function csvBlob(csv: string): Blob {
  return new Blob([`﻿${csv}`], { type: "text/csv;charset=utf-8" });
}

export async function downloadXlsx(context: ReportContext): Promise<void> {
  // ~90 kB of spreadsheet writer, loaded only when someone actually exports.
  const { default: writeXlsxFile } = await import("write-excel-file/browser");

  const header = HEADERS.map((value) => ({ value, fontWeight: "bold" as const }));
  const body = context.summaries.map((summary) => [
    // Names and student numbers are user-supplied, so they are neutralised before they land in
    // a cell that Excel would otherwise be willing to evaluate as a formula.
    { type: String, value: escapeSpreadsheetCell(summary.student.studentNumber) },
    { type: String, value: escapeSpreadsheetCell(summary.displayName) },
    { type: String, value: summary.student.gender === "male" ? "Boy" : "Girl" },
    { type: Number, value: summary.counts.present },
    { type: Number, value: summary.counts.late },
    { type: Number, value: summary.counts.absent },
    { type: Number, value: summary.sessions },
    { type: Number, value: Math.round(summary.rate * 100) / 100, format: "0%" },
  ]);

  await writeXlsxFile([header, ...body], {
    columns: [
      { width: 16 },
      { width: 30 },
      { width: 8 },
      { width: 10 },
      { width: 10 },
      { width: 10 },
      { width: 11 },
      { width: 16 },
    ],
    sheet: "Attendance",
    stickyRowsCount: 1,
  }).toFile(reportFilename(context, "xlsx"));
}

export function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}
