"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { ArrowLeft, Pencil, Plus, QrCode, Search, Trash2, UserPlus } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field, SelectInput, TextInput } from "@/components/ui/Field";
import { LinkButton } from "@/components/ui/LinkButton";
import { errorMessage, useToast } from "@/components/ui/Toast";
import { GENDERS, fullName, type Gender, type Student } from "@/domain/model";
import { getSection } from "@/lib/repositories/sections";
import {
  archiveStudent,
  createStudent,
  listStudentsBySection,
  searchStudents,
  updateStudent,
} from "@/lib/repositories/students";
import { QrCodeDialog } from "@/features/students/QrCodeDialog";

export function StudentsView({ sectionId }: { sectionId: number }) {
  const toast = useToast();
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Student | "new" | null>(null);
  const [showQrFor, setShowQrFor] = useState<Student | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Student | null>(null);

  const section = useLiveQuery(() => getSection(sectionId), [sectionId], undefined);
  const students = useLiveQuery(
    () => listStudentsBySection(sectionId),
    [sectionId],
    undefined,
  );

  const visible = useMemo(() => searchStudents(students ?? [], query), [students, query]);

  async function handleDelete() {
    if (!pendingDelete?.id) return;
    try {
      await archiveStudent(pendingDelete.id);
      toast.success(`Removed ${fullName(pendingDelete)}.`);
    } catch (error) {
      toast.error(errorMessage(error, "Could not remove that student."));
    } finally {
      setPendingDelete(null);
    }
  }

  if (section === null || (students !== undefined && section === undefined)) {
    return (
      <Card>
        <EmptyState
          icon={UserPlus}
          title="Section not found"
          description="It may have been removed. Pick another from the sections list."
          action={<LinkButton href="/sections">Back to sections</LinkButton>}
        />
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={section?.name ?? "Section"}
        description={`${students?.length ?? 0} active students`}
        actions={
          <>
            <LinkButton href="/sections" icon={<ArrowLeft aria-hidden className="size-4" />}>
              Sections
            </LinkButton>
            <Button
              variant="primary"
              icon={<Plus aria-hidden className="size-4" />}
              onClick={() => setEditing("new")}
            >
              Add student
            </Button>
          </>
        }
      />

      <Card>
        <CardBody className="border-b border-border">
          <label className="relative block">
            <span className="sr-only">Search students</span>
            <Search
              aria-hidden
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-subtle"
            />
            <TextInput
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by name or student number"
              className="pl-9"
            />
          </label>
        </CardBody>

        {visible.length === 0 ? (
          <EmptyState
            icon={UserPlus}
            title={query ? "No matches" : "No students yet"}
            description={
              query
                ? "No student in this section matches that search."
                : "Add the students who belong to this section. Each one gets a QR code to scan."
            }
            action={
              query ? undefined : (
                <Button variant="primary" onClick={() => setEditing("new")}>
                  Add student
                </Button>
              )
            }
          />
        ) : (
          <ul className="divide-y divide-border">
            <AnimatePresence initial={false}>
              {visible.map((student, index) => (
                <motion.li
                  key={student.id}
                  layout
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{
                    duration: 0.2,
                    delay: Math.min(index * 0.02, 0.3),
                    ease: [0.22, 1, 0.36, 1],
                  }}
                  className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-5"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-ink">{fullName(student)}</p>
                    <p className="truncate font-mono text-xs text-subtle">
                      {student.studentNumber} · {student.gender === "male" ? "Boy" : "Girl"}
                    </p>
                  </div>
                  <div className="flex gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Show QR code for ${fullName(student)}`}
                      icon={<QrCode aria-hidden className="size-4" />}
                      onClick={() => setShowQrFor(student)}
                    />
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Edit ${fullName(student)}`}
                      icon={<Pencil aria-hidden className="size-4" />}
                      onClick={() => setEditing(student)}
                    />
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Remove ${fullName(student)}`}
                      icon={<Trash2 aria-hidden className="size-4" />}
                      onClick={() => setPendingDelete(student)}
                    />
                  </div>
                </motion.li>
              ))}
            </AnimatePresence>
          </ul>
        )}
      </Card>

      <StudentDialog
        target={editing}
        sectionId={sectionId}
        onClose={() => setEditing(null)}
        onSaved={(message) => {
          setEditing(null);
          toast.success(message);
        }}
      />

      <QrCodeDialog student={showQrFor} onClose={() => setShowQrFor(null)} />

      <ConfirmDialog
        open={pendingDelete !== null}
        title={`Remove ${pendingDelete ? fullName(pendingDelete) : "student"}?`}
        description="They disappear from the roster and from future attendance. Records already taken are kept, so past reports stay accurate."
        confirmLabel="Remove student"
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}

interface FormState {
  lastName: string;
  firstName: string;
  middleName: string;
  studentNumber: string;
  gender: Gender;
}

const EMPTY_FORM: FormState = {
  lastName: "",
  firstName: "",
  middleName: "",
  studentNumber: "",
  gender: "male",
};

function StudentDialog({
  target,
  sectionId,
  onClose,
  onSaved,
}: {
  target: Student | "new" | null;
  sectionId: number;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const isNew = target === "new";
  const student = target === "new" || target === null ? null : target;

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  const [lastTarget, setLastTarget] = useState<typeof target>(null);

  if (target !== lastTarget) {
    setLastTarget(target);
    setForm(
      student
        ? {
            lastName: student.lastName,
            firstName: student.firstName,
            middleName: student.middleName,
            studentNumber: student.studentNumber,
            gender: student.gender,
          }
        : EMPTY_FORM,
    );
    setError(undefined);
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit() {
    setSaving(true);
    setError(undefined);
    try {
      const payload = { ...form, sectionId };
      if (isNew) {
        await createStudent(payload);
        onSaved(`Added ${form.lastName}, ${form.firstName}.`);
      } else if (student?.id) {
        await updateStudent(student.id, payload);
        onSaved("Student updated.");
      }
    } catch (caught) {
      setError(errorMessage(caught, "Could not save that student."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      open={target !== null}
      onClose={onClose}
      size="lg"
      title={isNew ? "Add student" : "Edit student"}
      description="The student number is what the QR code carries, so it must be unique."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={submit} disabled={saving}>
            {saving ? "Saving…" : isNew ? "Add student" : "Save changes"}
          </Button>
        </>
      }
    >
      <form
        className="grid gap-4 sm:grid-cols-2"
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        <Field label="Last name">
          {({ id }) => (
            <TextInput
              id={id}
              autoFocus
              maxLength={60}
              value={form.lastName}
              onChange={(event) => set("lastName", event.target.value)}
            />
          )}
        </Field>
        <Field label="First name">
          {({ id }) => (
            <TextInput
              id={id}
              maxLength={60}
              value={form.firstName}
              onChange={(event) => set("firstName", event.target.value)}
            />
          )}
        </Field>
        <Field label="Middle name" hint="Optional">
          {({ id }) => (
            <TextInput
              id={id}
              maxLength={60}
              value={form.middleName}
              onChange={(event) => set("middleName", event.target.value)}
            />
          )}
        </Field>
        <Field label="Gender">
          {({ id }) => (
            <SelectInput
              id={id}
              value={form.gender}
              onChange={(event) => set("gender", event.target.value as Gender)}
            >
              {GENDERS.map((gender) => (
                <option key={gender} value={gender}>
                  {gender === "male" ? "Boy" : "Girl"}
                </option>
              ))}
            </SelectInput>
          )}
        </Field>
        <div className="sm:col-span-2">
          <Field
            label="Student number"
            error={error}
            hint="Letters, digits, dots, hyphens, and underscores only"
          >
            {({ id, describedBy, invalid }) => (
              <TextInput
                id={id}
                maxLength={32}
                inputMode="text"
                autoComplete="off"
                className="font-mono"
                placeholder="2024-1001"
                aria-describedby={describedBy}
                aria-invalid={invalid}
                value={form.studentNumber}
                onChange={(event) => set("studentNumber", event.target.value)}
              />
            )}
          </Field>
        </div>
      </form>
    </Dialog>
  );
}
