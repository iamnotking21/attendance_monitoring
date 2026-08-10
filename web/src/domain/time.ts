import type { IsoDate, Time24 } from "./primitives";

/**
 * Everything here works in the device's local calendar on purpose. A school day is a local
 * concept: converting to UTC would push an evening record into the next day for anyone east of
 * Greenwich, and silently corrupt every report built on top of it.
 */

const MINUTES_PER_DAY = 24 * 60;

export function toIsoDate(date: Date): IsoDate {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function fromIsoDate(date: IsoDate): Date {
  const [year, month, day] = date.split("-").map(Number);
  return new Date(year, month - 1, day);
}

export function today(now: Date = new Date()): IsoDate {
  return toIsoDate(now);
}

/** Minutes elapsed since local midnight — the unit the window state machine compares in. */
export function minutesOfDay(now: Date = new Date()): number {
  return now.getHours() * 60 + now.getMinutes();
}

export function timeToMinutes(time: Time24): number {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + minutes;
}

export function minutesToTime(minutes: number): Time24 {
  const wrapped = ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY;
  const hours = String(Math.floor(wrapped / 60)).padStart(2, "0");
  const mins = String(wrapped % 60).padStart(2, "0");
  return `${hours}:${mins}`;
}

/** `14:05` renders as `2:05 PM` — the format the original app and its users expect. */
export function formatTime12(time: Time24): string {
  const [hours, minutes] = time.split(":").map(Number);
  const period = hours < 12 ? "AM" : "PM";
  const hour12 = hours % 12 === 0 ? 12 : hours % 12;
  return `${hour12}:${String(minutes).padStart(2, "0")} ${period}`;
}

/**
 * Parses the legacy Android format `h:mm:AM` (for example `7:30:AM`) into `HH:mm`.
 * Used only by the legacy-import adapter. Returns null on anything it does not recognise.
 */
export function parseLegacyTime(value: string): Time24 | null {
  const match = /^(\d{1,2}):([0-5]\d):(AM|PM)$/i.exec(value.trim());
  if (!match) return null;

  const [, rawHour, minutes, rawPeriod] = match;
  const hour = Number(rawHour);
  if (hour < 1 || hour > 12) return null;

  const period = rawPeriod.toUpperCase();
  const hours24 = period === "AM" ? hour % 12 : (hour % 12) + 12;
  return `${String(hours24).padStart(2, "0")}:${minutes}`;
}

export function formatDateLong(date: IsoDate): string {
  return fromIsoDate(date).toLocaleDateString(undefined, {
    weekday: "short",
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function formatDateShort(date: IsoDate): string {
  return fromIsoDate(date).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  });
}

export function addDays(date: IsoDate, days: number): IsoDate {
  const shifted = fromIsoDate(date);
  shifted.setDate(shifted.getDate() + days);
  return toIsoDate(shifted);
}

/** Inclusive on both ends. Returns [] if the range is inverted. */
export function datesBetween(start: IsoDate, end: IsoDate): IsoDate[] {
  if (start > end) return [];
  const dates: IsoDate[] = [];
  for (let cursor = start; cursor <= end; cursor = addDays(cursor, 1)) {
    dates.push(cursor);
  }
  return dates;
}

export function isWithinRange(date: IsoDate, start: IsoDate, end: IsoDate): boolean {
  return date >= start && date <= end;
}

/** First and last day of the month containing `date`. */
export function monthRange(date: IsoDate): { start: IsoDate; end: IsoDate } {
  const [year, month] = date.split("-").map(Number);
  const start = toIsoDate(new Date(year, month - 1, 1));
  const end = toIsoDate(new Date(year, month, 0));
  return { start, end };
}
