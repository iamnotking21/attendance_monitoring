import { fullName, studentInputSchema, type Student } from "@/domain/model";
import { newId, now } from "@/domain/primitives";
import { db } from "@/lib/db";

export class DuplicateStudentNumberError extends Error {
  constructor(studentNumber: string) {
    super(`Student number "${studentNumber}" is already in use.`);
    this.name = "DuplicateStudentNumberError";
  }
}

export async function listStudentsBySection(sectionId: string): Promise<Student[]> {
  const students = await db().students.where({ sectionId }).toArray();
  return students
    .filter((student) => !student.archived)
    .sort((a, b) => fullName(a).localeCompare(fullName(b)));
}

export async function listAllStudents(): Promise<Student[]> {
  const students = await db().students.toArray();
  return students.filter((student) => !student.archived);
}

export async function countStudentsBySection(sectionId: string): Promise<number> {
  const students = await db().students.where({ sectionId }).toArray();
  return students.filter((student) => !student.archived).length;
}

/** Resolves a scanned QR payload to a student. Archived students never match. */
export async function findActiveByStudentNumber(
  studentNumber: string,
): Promise<Student | undefined> {
  const matches = await db().students.where({ studentNumber }).toArray();
  return matches.find((student) => !student.archived);
}

export async function createStudent(input: unknown): Promise<string> {
  const student = studentInputSchema.parse(input);
  const timestamp = now();

  return db().transaction("rw", db().students, async () => {
    if (await isStudentNumberTaken(student.studentNumber)) {
      throw new DuplicateStudentNumberError(student.studentNumber);
    }
    return db().students.add({
      ...student,
      id: newId(),
      archived: false,
      createdAt: timestamp,
      updatedAt: timestamp,
    });
  });
}

export async function updateStudent(id: string, input: unknown): Promise<void> {
  const student = studentInputSchema.parse(input);

  await db().transaction("rw", db().students, async () => {
    if (await isStudentNumberTaken(student.studentNumber, id)) {
      throw new DuplicateStudentNumberError(student.studentNumber);
    }
    await db().students.update(id, { ...student, updatedAt: now() });
  });
}

export async function archiveStudent(id: string): Promise<void> {
  await db().students.update(id, { archived: true, updatedAt: now() });
}

/**
 * Matches on name or student number. Deliberately a plain substring test rather than a stored
 * index: a roster is a few hundred rows, and an index would buy nothing but complexity.
 */
export function searchStudents(students: readonly Student[], query: string): Student[] {
  const needle = query.trim().toLocaleLowerCase();
  if (needle === "") return [...students];

  return students.filter(
    (student) =>
      student.studentNumber.toLocaleLowerCase().includes(needle) ||
      fullName(student).toLocaleLowerCase().includes(needle),
  );
}

async function isStudentNumberTaken(
  studentNumber: string,
  exceptId?: string,
): Promise<boolean> {
  const matches = await db().students.where({ studentNumber }).toArray();
  return matches.some((student) => !student.archived && student.id !== exceptId);
}
