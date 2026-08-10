import { sectionInputSchema, type Section } from "@/domain/model";
import { db } from "@/lib/db";

export async function listSections(): Promise<Section[]> {
  const sections = await db().sections.toArray();
  return sections
    .filter((section) => !section.archived)
    .sort((a, b) => a.name.localeCompare(b.name));
}

export async function getSection(id: number): Promise<Section | undefined> {
  return db().sections.get(id);
}

export class DuplicateSectionError extends Error {
  constructor(name: string) {
    super(`A section named "${name}" already exists.`);
    this.name = "DuplicateSectionError";
  }
}

export async function createSection(input: unknown): Promise<number> {
  const { name } = sectionInputSchema.parse(input);

  return db().transaction("rw", db().sections, async () => {
    if (await hasSectionNamed(name)) throw new DuplicateSectionError(name);
    return db().sections.add({
      name,
      archived: false,
      createdAt: new Date().toISOString(),
    });
  });
}

export async function renameSection(id: number, input: unknown): Promise<void> {
  const { name } = sectionInputSchema.parse(input);

  await db().transaction("rw", db().sections, async () => {
    if (await hasSectionNamed(name, id)) throw new DuplicateSectionError(name);
    await db().sections.update(id, { name });
  });
}

/**
 * Soft delete. Attendance history references the section, and hard-deleting it would silently
 * rewrite past reports — so the section leaves the UI while its records stay intact.
 */
export async function archiveSection(id: number): Promise<void> {
  await db().transaction("rw", db().sections, db().students, db().schedules, async () => {
    await db().sections.update(id, { archived: true });
    await db().students.where({ sectionId: id }).modify({ archived: true });
    await db().schedules.where({ sectionId: id }).modify({ archived: true });
  });
}

async function hasSectionNamed(name: string, exceptId?: number): Promise<boolean> {
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
