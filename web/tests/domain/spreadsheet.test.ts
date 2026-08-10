import { describe, expect, it } from "vitest";

import {
  escapeSpreadsheetCell,
  toCsv,
  toCsvCell,
  toSafeFilename,
} from "@/domain/spreadsheet";

describe("escapeSpreadsheetCell", () => {
  it.each(["=1+1", "+1", "-1", "@SUM(A1)", "\tvalue", "\rvalue"])(
    "neutralises %j so Excel treats it as text",
    (value) => {
      expect(escapeSpreadsheetCell(value)).toBe(`'${value}`);
    },
  );

  it("neutralises the classic CSV-injection payload", () => {
    const payload = `=cmd|'/c calc'!A1`;
    expect(escapeSpreadsheetCell(payload).startsWith("'=")).toBe(true);
  });

  it("leaves ordinary values untouched", () => {
    expect(escapeSpreadsheetCell("Dela Cruz, Juan")).toBe("Dela Cruz, Juan");
    expect(escapeSpreadsheetCell("2024-1001")).toBe("2024-1001");
    // A hyphen inside the value is harmless; only a leading one starts a formula.
    expect(escapeSpreadsheetCell("Grade 11 - Rizal")).toBe("Grade 11 - Rizal");
  });
});

describe("toCsvCell", () => {
  it("quotes values containing a comma, a quote, or a newline", () => {
    expect(toCsvCell("Dela Cruz, Juan")).toBe('"Dela Cruz, Juan"');
    expect(toCsvCell('He said "hi"')).toBe('"He said ""hi"""');
    expect(toCsvCell("line1\nline2")).toBe('"line1\nline2"');
  });

  it("renders empty for null and undefined", () => {
    expect(toCsvCell(null)).toBe("");
    expect(toCsvCell(undefined)).toBe("");
  });

  it("passes finite numbers through and drops non-finite ones", () => {
    expect(toCsvCell(42)).toBe("42");
    expect(toCsvCell(Number.NaN)).toBe("");
    expect(toCsvCell(Number.POSITIVE_INFINITY)).toBe("");
  });

  it("escapes a formula and then quotes it", () => {
    expect(toCsvCell("=1+1,2")).toBe(`"'=1+1,2"`);
  });
});

describe("toCsv", () => {
  it("joins rows with CRLF, as RFC 4180 specifies", () => {
    expect(
      toCsv([
        ["a", "b"],
        ["c", "d"],
      ]),
    ).toBe("a,b\r\nc,d");
  });
});

describe("toSafeFilename", () => {
  it("strips path separators and reserved characters", () => {
    expect(toSafeFilename("../../etc/passwd")).toBe("etc-passwd");
    expect(toSafeFilename('a:b*c?d"e<f>g|h')).toBe("a-b-c-d-e-f-g-h");
  });

  it("collapses whitespace and trims stray separators", () => {
    expect(toSafeFilename("Grade 11 - Rizal")).toBe("Grade-11-Rizal");
  });

  it("falls back when nothing usable is left", () => {
    expect(toSafeFilename("///", "export")).toBe("export");
    expect(toSafeFilename("", "export")).toBe("export");
  });
});
