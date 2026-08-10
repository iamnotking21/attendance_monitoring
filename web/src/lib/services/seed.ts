import type { Gender, New, NewAttendanceRecord, Schedule, Section, Student } from "@/domain/model";
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

export async function seedDemoData(now: Date = new Date()): Promise<boolean> {
  if (!(await isDatabaseEmpty())) return false;

  const createdAt = now.toISOString();
  const random = createRandom(20_240_617);

  const sectionIds = await db().sections.bulkAdd(
    SECTION_NAMES.map<New<Section>>((name) => ({ name, archived: false, createdAt })),
    { allKeys: true },
  );

  const students: New<Student>[] = ROSTER.map(([lastName, firstName, middleName, gender], index) => {
    const sectionIndex = index < 18 ? 0 : 1;
    return {
      sectionId: sectionIds[sectionIndex],
      studentNumber: `2024-${String(1001 + index).padStart(4, "0")}`,
      lastName,
      firstName,
      middleName,
      gender,
      archived: false,
      createdAt,
    };
  });
  await db().students.bulkAdd(students);

  // One window straddling the current moment, so the scanner demo actually records something,
  // and one fixed morning window that supplies the historical data.
  const currentMinute = now.getHours() * 60 + now.getMinutes();
  const schedules: New<Schedule>[] = [
    {
      sectionId: sectionIds[0],
      title: "Morning Assembly",
      venue: "Quadrangle",
      present: { start: "07:00", end: "07:30" },
      late: { start: "07:30", end: "08:00" },
      archived: false,
      createdAt,
    },
    {
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
    },
    {
      sectionId: sectionIds[1],
      title: "Morning Assembly",
      venue: "Quadrangle",
      present: { start: "07:00", end: "07:30" },
      late: { start: "07:30", end: "08:00" },
      archived: false,
      createdAt,
    },
  ];
  const scheduleIds = await db().schedules.bulkAdd(schedules, { allKeys: true });

  // Four weeks of weekday history for the two Morning Assembly schedules.
  const records: NewAttendanceRecord[] = [];
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
          scheduleId: scheduleIds[scheduleIndex],
          sectionId: student.sectionId,
          studentNumber: student.studentNumber,
          date,
          status,
          scheduleTitle: schedules[scheduleIndex].title,
          recordedAt: `${date}T07:${String(Math.floor(random() * 55)).padStart(2, "0")}:00`,
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
