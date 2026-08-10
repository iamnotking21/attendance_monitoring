import { describe, expect, it } from "vitest";

import {
  addDays,
  datesBetween,
  formatTime12,
  minutesOfDay,
  minutesToTime,
  monthRange,
  parseLegacyTime,
  timeToMinutes,
  toIsoDate,
  today,
} from "@/domain/time";

describe("toIsoDate", () => {
  it("formats in local time, not UTC", () => {
    // 23:30 local on 15 March. A UTC conversion would report the 16th anywhere east of
    // Greenwich, which would file an evening scan under the wrong school day.
    expect(toIsoDate(new Date(2024, 2, 15, 23, 30))).toBe("2024-03-15");
  });

  it("pads single-digit months and days", () => {
    expect(toIsoDate(new Date(2024, 0, 5))).toBe("2024-01-05");
  });

  it("agrees with today()", () => {
    const now = new Date(2024, 5, 1, 12, 0);
    expect(today(now)).toBe(toIsoDate(now));
  });
});

describe("minutesOfDay", () => {
  it("counts from local midnight", () => {
    expect(minutesOfDay(new Date(2024, 2, 15, 0, 0))).toBe(0);
    expect(minutesOfDay(new Date(2024, 2, 15, 7, 30))).toBe(450);
    expect(minutesOfDay(new Date(2024, 2, 15, 23, 59))).toBe(1439);
  });
});

describe("timeToMinutes / minutesToTime", () => {
  it("round-trips every minute of the day", () => {
    for (let minute = 0; minute < 1440; minute += 1) {
      expect(timeToMinutes(minutesToTime(minute))).toBe(minute);
    }
  });

  it("wraps values outside the day rather than producing invalid times", () => {
    expect(minutesToTime(-30)).toBe("23:30");
    expect(minutesToTime(1500)).toBe("01:00");
  });
});

describe("formatTime12", () => {
  it.each([
    ["00:00", "12:00 AM"],
    ["00:05", "12:05 AM"],
    ["07:30", "7:30 AM"],
    ["12:00", "12:00 PM"],
    ["12:45", "12:45 PM"],
    ["13:05", "1:05 PM"],
    ["23:59", "11:59 PM"],
  ])("renders %s as %s", (input, expected) => {
    expect(formatTime12(input)).toBe(expected);
  });
});

describe("parseLegacyTime", () => {
  it("reads the Android app's hh:mm:AM format", () => {
    expect(parseLegacyTime("7:30:AM")).toBe("07:30");
    expect(parseLegacyTime("12:00:AM")).toBe("00:00");
    expect(parseLegacyTime("12:15:PM")).toBe("12:15");
    expect(parseLegacyTime("1:05:PM")).toBe("13:05");
  });

  it("rejects anything it does not recognise", () => {
    expect(parseLegacyTime("25:00:AM")).toBeNull();
    expect(parseLegacyTime("0:30:AM")).toBeNull();
    expect(parseLegacyTime("07:30")).toBeNull();
    expect(parseLegacyTime("")).toBeNull();
  });
});

describe("date ranges", () => {
  it("addDays crosses month and year boundaries", () => {
    expect(addDays("2024-02-28", 1)).toBe("2024-02-29");
    expect(addDays("2023-02-28", 1)).toBe("2023-03-01");
    expect(addDays("2024-12-31", 1)).toBe("2025-01-01");
    expect(addDays("2024-01-01", -1)).toBe("2023-12-31");
  });

  it("datesBetween is inclusive on both ends", () => {
    expect(datesBetween("2024-03-01", "2024-03-03")).toEqual([
      "2024-03-01",
      "2024-03-02",
      "2024-03-03",
    ]);
    expect(datesBetween("2024-03-01", "2024-03-01")).toEqual(["2024-03-01"]);
  });

  it("datesBetween returns nothing for an inverted range", () => {
    expect(datesBetween("2024-03-03", "2024-03-01")).toEqual([]);
  });

  it("monthRange finds the real last day, including in a leap year", () => {
    expect(monthRange("2024-02-15")).toEqual({ start: "2024-02-01", end: "2024-02-29" });
    expect(monthRange("2023-02-15")).toEqual({ start: "2023-02-01", end: "2023-02-28" });
    expect(monthRange("2024-12-09")).toEqual({ start: "2024-12-01", end: "2024-12-31" });
  });
});
