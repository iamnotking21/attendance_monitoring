import { z } from "zod";

/** Calendar date in `YYYY-MM-DD`, always interpreted in the device's local timezone. */
export const isoDateSchema = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD")
  .refine((value) => {
    const [year, month, day] = value.split("-").map(Number);
    const date = new Date(year, month - 1, day);
    return (
      date.getFullYear() === year &&
      date.getMonth() === month - 1 &&
      date.getDate() === day
    );
  }, "Not a real calendar date");

/** Wall-clock time in 24-hour `HH:mm`. */
export const time24Schema = z
  .string()
  .regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Time must be HH:mm (24-hour)");

/**
 * Free text supplied by a user. Trimmed, length-bounded, and stripped of control characters
 * so that nothing exotic reaches storage or an exported spreadsheet.
 */
export const displayText = (label: string, max: number) =>
  z
    .string()
    .transform((value) => value.replace(/[\p{Cc}\p{Cf}]/gu, "").trim())
    .pipe(
      z
        .string()
        .min(1, `${label} is required`)
        .max(max, `${label} must be ${max} characters or fewer`),
    );

export const optionalDisplayText = (label: string, max: number) =>
  z
    .string()
    .transform((value) => value.replace(/[\p{Cc}\p{Cf}]/gu, "").trim())
    .pipe(z.string().max(max, `${label} must be ${max} characters or fewer`))
    .default("");

/**
 * A student number doubles as the QR payload, so it is deliberately narrow: letters, digits,
 * and separators only. This is the primary defence against a hostile QR code — anything that
 * could be read as markup, a URL, or a spreadsheet formula never becomes a valid lookup key.
 */
export const studentNumberSchema = z
  .string()
  .transform((value) => value.trim())
  .pipe(
    z
      .string()
      .min(1, "Student number is required")
      .max(32, "Student number must be 32 characters or fewer")
      .regex(
        /^[A-Za-z0-9][A-Za-z0-9._-]*$/,
        "Student number may contain only letters, digits, dots, hyphens, and underscores",
      ),
  );

export const idSchema = z.number().int().positive();

export type IsoDate = z.infer<typeof isoDateSchema>;
export type Time24 = z.infer<typeof time24Schema>;
export type Id = z.infer<typeof idSchema>;
