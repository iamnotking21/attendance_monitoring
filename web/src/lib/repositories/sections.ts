import { sectionInputSchema, type Section } from "@/domain/model";
import { newId, now } from "@/domain/primitives";
import { db } from "@/lib/db";

export async function listSections(): Promise<Section[]> {
  const sections = await db().sections.toArray();
  return sections
    .filter((section) => !section.archived)
    .sort((a, b) => a.name.localeCompare(b.name));
}

export async function getSection(id: string): Promise<Section | undefined> {
  return db().sections.get(id);
}

export class DuplicateSectionError extends Error {
  constructor(name: string) {
    super(`A section named "${name}" already exists.`);
    this.name = "DuplicateSectionError";
  }
}

export async function createSection(input: unknown): Promise<string> {
  const { name } = sectionInputSchema.parse(input);
  const timestamp = now();

  return db().transaction("rw", db().sections, async () => {
    if (await hasSectionNamed(name)) throw new DuplicateSectionError(name);
    return db().sections.add({
      id: newId(),
      name,
      archived: false,
      createdAt: timestamp,
      updatedAt: timestamp,
    });
  });
}

export async function renameSection(id: string, input: unknown): Promise<void> {
  const { name } = sectionInputSchema.parse(input);

  await db().transaction("rw", db().sections, async () => {
    if (await hasSectionNamed(name, id)) throw new DuplicateSectionError(name);
    await db().sections.update(id, { name, updatedAt: now() });
  });
}

/**
 * Soft delete. Attendance history references the section, and hard-deleting it would silently
 * rewrite past reports — so the section leaves the UI while its records stay intact. It is also
 * what makes deletion syncable: a removed row still exists to be replicated.
 */
export async function archiveSection(id: string): Promise<void> {
  const timestamp = now();

  await db().transaction("rw", db().sections, db().students, db().schedules, async () => {
    await db().sections.update(id, { archived: true, updatedAt: timestamp });
    await db()
      .students.where({ sectionId: id })
      .modify({ archived: true, updatedAt: timestamp });
    await db()
      .schedules.where({ sectionId: id })
      .modify({ archived: true, updatedAt: timestamp });
  });
}

async function hasSectionNamed(name: string, exceptId?: string): Promise<boolean> {
  const needle = name.toLocaleLowerCase();
  const clash = await db()
    .sections.filter(
      (section) =>
        !section.archived &&
        section.id !== exceptId &&
        section.name.toLocaleLowerCase() === needle,
    )
    .first();
  return clash !== undefined;
}
