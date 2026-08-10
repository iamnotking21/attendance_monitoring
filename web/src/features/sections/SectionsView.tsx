"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { ChevronRight, Pencil, Plus, Trash2, Users } from "lucide-react";
import { AnimatePresence, m } from "motion/react";
import Link from "next/link";
import { useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field, TextInput } from "@/components/ui/Field";
import { errorMessage, useToast } from "@/components/ui/Toast";
import type { Section } from "@/domain/model";
import { db } from "@/lib/db";
import {
  archiveSection,
  createSection,
  listSections,
  renameSection,
} from "@/lib/repositories/sections";

export function SectionsView() {
  const toast = useToast();
  const [editing, setEditing] = useState<Section | "new" | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Section | null>(null);

  const sections = useLiveQuery(() => listSections(), [], undefined);
  const counts = useLiveQuery(
    async () => {
      const students = await db().students.toArray();
      const map = new Map<string, number>();
      for (const student of students) {
        if (student.archived) continue;
        map.set(student.sectionId, (map.get(student.sectionId) ?? 0) + 1);
      }
      return map;
    },
    [],
    new Map<string, number>(),
  );

  async function handleDelete() {
    if (!pendingDelete?.id) return;
    try {
      await archiveSection(pendingDelete.id);
      toast.success(`Removed ${pendingDelete.name}.`);
    } catch (error) {
      toast.error(errorMessage(error, "Could not remove that section."));
    } finally {
      setPendingDelete(null);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Sections"
        description="A section is one class list. Students, schedules, and records all hang off it."
        actions={
          <Button
            variant="primary"
            icon={<Plus aria-hidden className="size-4" />}
            onClick={() => setEditing("new")}
          >
            New section
          </Button>
        }
      />

      {sections && sections.length === 0 ? (
        <Card>
          <EmptyState
            icon={Users}
            title="No sections yet"
            description="Create your first section, then add the students who belong to it."
            action={
              <Button variant="primary" onClick={() => setEditing("new")}>
                New section
              </Button>
            }
          />
        </Card>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <AnimatePresence initial={false}>
            {(sections ?? []).map((section, index) => (
              <m.li
                key={section.id}
                layout
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.97 }}
                transition={{
                  duration: 0.24,
                  delay: Math.min(index * 0.03, 0.3),
                  ease: [0.22, 1, 0.36, 1],
                }}
              >
                <Card className="group flex h-full flex-col justify-between gap-4 p-4 transition-colors duration-150 hover:border-border-strong">
                  <Link
                    href={`/sections/${section.id}`}
                    className="flex items-start justify-between gap-3"
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-base font-semibold tracking-tight text-ink">
                        {section.name}
                      </span>
                      <span className="mt-0.5 block text-sm text-muted tabular">
                        {counts?.get(section.id) ?? 0} students
                      </span>
                    </span>
                    <ChevronRight
                      aria-hidden
                      className="mt-1 size-4 shrink-0 text-subtle transition-transform duration-150 group-hover:translate-x-0.5 group-hover:text-primary"
                    />
                  </Link>

                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      icon={<Pencil aria-hidden className="size-3.5" />}
                      onClick={() => setEditing(section)}
                    >
                      Rename
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Trash2 aria-hidden className="size-3.5" />}
                      onClick={() => setPendingDelete(section)}
                    >
                      Remove
                    </Button>
                  </div>
                </Card>
              </m.li>
            ))}
          </AnimatePresence>
        </ul>
      )}

      <SectionDialog
        target={editing}
        onClose={() => setEditing(null)}
        onSaved={(message) => {
          setEditing(null);
          toast.success(message);
        }}
        onFailed={(message) => toast.error(message)}
      />

      <ConfirmDialog
        open={pendingDelete !== null}
        title={`Remove ${pendingDelete?.name ?? "section"}?`}
        description="Its students and schedules are removed with it. Attendance already recorded is kept, so past reports stay accurate."
        confirmLabel="Remove section"
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}

function SectionDialog({
  target,
  onClose,
  onSaved,
  onFailed,
}: {
  target: Section | "new" | null;
  onClose: () => void;
  onSaved: (message: string) => void;
  onFailed: (message: string) => void;
}) {
  const isNew = target === "new";
  const section = target === "new" || target === null ? null : target;
  const [name, setName] = useState("");
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);

  // Reset the field whenever a different target opens the dialog.
  const [lastTarget, setLastTarget] = useState<typeof target>(null);
  if (target !== lastTarget) {
    setLastTarget(target);
    setName(section?.name ?? "");
    setError(undefined);
  }

  async function submit() {
    setSaving(true);
    setError(undefined);
    try {
      if (isNew) {
        await createSection({ name });
        onSaved(`Created ${name.trim()}.`);
      } else if (section?.id) {
        await renameSection(section.id, { name });
        onSaved("Section renamed.");
      }
    } catch (caught) {
      const message = errorMessage(caught, "Could not save that section.");
      setError(message);
      onFailed(message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      open={target !== null}
      onClose={onClose}
      title={isNew ? "New section" : "Rename section"}
      description={isNew ? "For example: Grade 11 - Rizal" : undefined}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={submit} disabled={saving || name.trim() === ""}>
            {saving ? "Saving…" : isNew ? "Create section" : "Save"}
          </Button>
        </>
      }
    >
      <form
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        <Field label="Section name" error={error}>
          {({ id, describedBy, invalid }) => (
            <TextInput
              id={id}
              value={name}
              autoFocus
              maxLength={80}
              aria-describedby={describedBy}
              aria-invalid={invalid}
              onChange={(event) => setName(event.target.value)}
              placeholder="Grade 11 - Rizal"
            />
          )}
        </Field>
      </form>
    </Dialog>
  );
}
