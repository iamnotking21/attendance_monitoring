import { scheduleInputSchema, type Schedule } from "@/domain/model";
import { timeToMinutes } from "@/domain/time";
import { db } from "@/lib/db";

function byStartTime(a: Schedule, b: Schedule): number {
  return timeToMinutes(a.present.start) - timeToMinutes(b.present.start);
}

export async function listSchedulesBySection(sectionId: number): Promise<Schedule[]> {
  const schedules = await db().schedules.where({ sectionId }).toArray();
  return schedules.filter((schedule) => !schedule.archived).sort(byStartTime);
}

export async function listActiveSchedules(): Promise<Schedule[]> {
  const schedules = await db().schedules.toArray();
  return schedules.filter((schedule) => !schedule.archived).sort(byStartTime);
}

export async function getSchedule(id: number): Promise<Schedule | undefined> {
  return db().schedules.get(id);
}

export async function createSchedule(input: unknown): Promise<number> {
  const schedule = scheduleInputSchema.parse(input);
  return db().schedules.add({
    ...schedule,
    archived: false,
    createdAt: new Date().toISOString(),
  });
}

export async function updateSchedule(id: number, input: unknown): Promise<void> {
  const schedule = scheduleInputSchema.parse(input);
  await db().schedules.update(id, schedule);
}

/**
 * Soft delete, for the same reason sections use one: records carry a `scheduleId`, and removing
 * the row would orphan every past record that points at it.
 */
export async function archiveSchedule(id: number): Promise<void> {
  await db().schedules.update(id, { archived: true });
}
