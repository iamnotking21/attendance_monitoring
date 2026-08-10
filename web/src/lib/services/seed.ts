import type { AttendanceRecord, Gender, Schedule, Section, Student } from "@/domain/model";
import { newId } from "@/domain/primitives";
import { addDays, fromIsoDate, minutesToTime, today } from "@/domain/time";
import { db } from "@/lib/db";

/**
 * Demo data, so the deployed portfolio build shows a working system instead of six empty
 * screens. Only ever runs against an empty database — it must never overwrite real records.
 */

const SECTION_NAMES = ["Grade 11 - Rizal", "Grade 12 - Mabini"] as const;

const ROSTER: ReadonlyArray<readonly [string, string, string, Gender]> = [
  ["Dela Cruz", "Juan", "Ramos", "male"],
  ["Santos", "Maria", "Lopez", "female"],
  ["Reyes", "Jose", "Bautista", "male"],
  ["Bautista", "Ana", "Cruz", "female"],
  ["Garcia", "Miguel", "Torres", "male"],
  ["Mendoza", "Sofia", "Villanueva", "female"],
  ["Torres", "Gabriel", "Aquino", "male"],
  ["Villanueva", "Isabel", "Ramos", "female"],
  ["Aquino", "Rafael", "Santos", "male"],
  ["Ramos", "Camille", "Dizon", "female"],
  ["Castillo", "Andres", "Reyes", "male"],
  ["Domingo", "Patricia", "Gomez", "female"],
  ["Navarro", "Emilio", "Salazar", "male"],
  ["Salazar", "Bianca", "Navarro", "female"],
  ["Gomez", "Lorenzo", "Castro", "male"],
  ["Fernandez", "Angelica", "Rivera", "female"],
  ["Rivera", "Tomas", "Fernandez", "male"],
  ["Dizon", "Katrina", "Ocampo", "female"],
  ["Ocampo", "Diego", "Manalo", "male"],
  ["Manalo", "Trisha", "Alonzo", "female"],
  ["Alonzo", "Paolo", "Herrera", "male"],
  ["Herrera", "Danica", "Pascual", "female"],
  ["Pascual", "Enrique", "Lim", "male"],
  ["Lim", "Jasmine", "Bernardo", "female"],
  ["Bernardo", "Marco", "Espino", "male"],
  ["Espino", "Rowena", "Cordero", "female"],
  ["Cordero", "Vicente", "Padilla", "male"],
  ["Padilla", "Andrea", "Bautista", "female"],
  ["Soriano", "Julian", "Gatchalian", "male"],
  ["Gatchalian", "Michelle", "Soriano", "female"],
  ["Ignacio", "Ramon", "Velasco", "male"],
  ["Velasco", "Clarissa", "Ignacio", "female"],
  ["Mercado", "Adrian", "Sarmiento", "male"],
  ["Sarmiento", "Nicole", "Mercado", "female"],
] as const;

/**
 * A small linear congruential generator. Seeded rather than `Math.random` so the demo history
 * looks identical on every device — a screenshot in the README matches what a visitor sees.
 */
function createRandom(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 0x1_0000_0000;
  };
}

/**
 * A real ISO 8601 instant, not a date and time glued together. The sync protocol validates
 * timestamps strictly, and a string without a timezone is ambiguous — it was rejected by the
 * server, which silently stopped the entire first push.
 */
function recordedAtFor(date: string, minute: number): string {
  const [year, month, day] = date.split("-").map(Number);
  return new Date(year, month - 1, day, 7, minute, 0).toISOString();
}

function clampMinute(minute: number): number {
  return Math.max(0, Math.min(23 * 60 + 59, minute));
}

export async function isDatabaseEmpty(): Promise<boolean> {
  const [sections, students, records] = await Promise.all([
    db().sections.count(),
    db().students.count(),
    db().records.count(),
  ]);
  return sections === 0 && students === 0 && records === 0;
}

/**
 * De-duplicates concurrent callers within one page. React's development Strict Mode runs the
 * bootstrap effect twice, and without this both runs would be in flight before either had
 * written anything.
 */
let inFlight: Promise<boolean> | null = null;

export function seedDemoData(now: Date = new Date()): Promise<boolean> {
  inFlight ??= runSeed(now).finally(() => {
    inFlight = null;
  });
  return inFlight;
}

async function runSeed(now: Date): Promise<boolean> {
  const database = db();

  // The emptiness check and the writes share one transaction, so a second caller that slipped
  // past the in-flight guard — another tab, say — sees the seeded data and backs out. Checking
  // outside the transaction is what produced two copies of every section.
  return database.transaction(
    "rw",
    [
      database.sections,
      database.students,
      database.schedules,
      database.records,
      database.schoolDays,
    ],
    () => seedWithinTransaction(now),
  );
}

async function seedWithinTransaction(now: Date): Promise<boolean> {
  if (!(await isDatabaseEmpty())) return false;

  const createdAt = now.toISOString();
  const random = createRandom(20_240_617);

  // Identifiers are minted here rather than assigned by storage, so the foreign keys below can
  // be wired up before anything is written.
  const sectionIds = SECTION_NAMES.map(() => newId());
  const sections: Section[] = SECTION_NAMES.map((name, index) => ({
    id: sectionIds[index],
    name,
    archived: false,
    createdAt,
    updatedAt: createdAt,
  }));
  await db().sections.bulkAdd(sections);

  const students: Student[] = ROSTER.map(([lastName, firstName, middleName, gender], index) => {
    const sectionIndex = index < 18 ? 0 : 1;
    return {
      id: newId(),
      sectionId: sectionIds[sectionIndex],
      studentNumber: `2024-${String(1001 + index).padStart(4, "0")}`,
      lastName,
      firstName,
      middleName,
      gender,
      archived: false,
      createdAt,
      updatedAt: createdAt,
    };
  });
  await db().students.bulkAdd(students);

  // One window straddling the current moment, so the scanner demo actually records something,
  // and one fixed morning window that supplies the historical data.
  const currentMinute = now.getHours() * 60 + now.getMinutes();
  const scheduleIds = [newId(), newId(), newId()];
  const schedules: Schedule[] = [
    {
      id: scheduleIds[0],
      sectionId: sectionIds[0],
      title: "Morning Assembly",
      venue: "Quadrangle",
      present: { start: "07:00", end: "07:30" },
      late: { start: "07:30", end: "08:00" },
      archived: false,
      createdAt,
      updatedAt: createdAt,
    },
    {
      id: scheduleIds[1],
      sectionId: sectionIds[0],
      title: "Homeroom (live demo)",
      venue: "Room 201",
      present: {
        start: minutesToTime(clampMinute(currentMinute - 30)),
        end: minutesToTime(clampMinute(currentMinute + 30)),
      },
      late: {
        start: minutesToTime(clampMinute(currentMinute + 30)),
        end: minutesToTime(clampMinute(currentMinute + 90)),
      },
      archived: false,
      createdAt,
      updatedAt: createdAt,
    },
    {
      id: scheduleIds[2],
      sectionId: sectionIds[1],
      title: "Morning Assembly",
      venue: "Quadrangle",
      present: { start: "07:00", end: "07:30" },
      late: { start: "07:30", end: "08:00" },
      archived: false,
      createdAt,
      updatedAt: createdAt,
    },
  ];
  await db().schedules.bulkAdd(schedules);

  // Four weeks of weekday history for the two Morning Assembly schedules.
  const records: AttendanceRecord[] = [];
  const schoolDays: { date: string; firstSeenAt: string }[] = [];
  const start = addDays(today(now), -27);

  for (let offset = 0; offset < 28; offset += 1) {
    const date = addDays(start, offset);
    const weekday = fromIsoDate(date).getDay();
    if (weekday === 0 || weekday === 6) continue;

    schoolDays.push({ date, firstSeenAt: createdAt });

    for (const [sectionIndex, scheduleIndex] of [
      [0, 0],
      [1, 2],
    ] as const) {
      for (const student of students) {
        if (student.sectionId !== sectionIds[sectionIndex]) continue;

        const roll = random();
        const status = roll < 0.86 ? "present" : roll < 0.94 ? "late" : "absent";
        records.push({
          id: newId(),
          scheduleId: scheduleIds[scheduleIndex],
          sectionId: student.sectionId,
          studentNumber: student.studentNumber,
          date,
          status,
          scheduleTitle: schedules[scheduleIndex].title,
          recordedAt: recordedAtFor(date, Math.floor(random() * 55)),
        });
      }
    }
  }

  const currentDate = today(now);
  if (!schoolDays.some((day) => day.date === currentDate)) {
    schoolDays.push({ date: currentDate, firstSeenAt: createdAt });
  }

  await db().schoolDays.bulkAdd(schoolDays);
  await db().records.bulkAdd(records);

  return true;
}
