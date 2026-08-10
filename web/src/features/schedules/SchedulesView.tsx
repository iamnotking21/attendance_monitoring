"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { CalendarClock, MapPin, Pencil, Plus, Trash2 } from "lucide-react";
import { AnimatePresence, m } from "motion/react";
import { useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field, TextInput } from "@/components/ui/Field";
import { LinkButton } from "@/components/ui/LinkButton";
import { SectionPicker } from "@/components/ui/SectionPicker";
import { errorMessage, useToast } from "@/components/ui/Toast";
import { windowStateAt } from "@/domain/attendance";
import type { Schedule } from "@/domain/model";
import { formatTime12, minutesOfDay } from "@/domain/time";
import {
  archiveSchedule,
  createSchedule,
  listSchedulesBySection,
  updateSchedule,
} from "@/lib/repositories/schedules";
import { describeWindowState } from "@/lib/services/attendance";
import { useSectionSelection } from "@/features/shared/useSectionSelection";
import { cn } from "@/lib/cn";

export function SchedulesView() {
  const toast = useToast();
  const { sections, selectedId, select, loading } = useSectionSelection();
  const [editing, setEditing] = useState<Schedule | "new" | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Schedule | null>(null);

  const schedules = useLiveQuery(
    () => (selectedId ? listSchedulesBySection(selectedId) : Promise.resolve([])),
    [selectedId],
    undefined,
  );

  async function handleDelete() {
    if (!pendingDelete?.id) return;
    try {
      await archiveSchedule(pendingDelete.id);
      toast.success(`Removed ${pendingDelete.title}.`);
    } catch (error) {
      toast.error(errorMessage(error, "Could not remove that schedule."));
    } finally {
      setPendingDelete(null);
    }
  }

  if (!loading && (!sections || sections.length === 0)) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title="Schedules" />
        <Card>
          <EmptyState
            icon={CalendarClock}
            title="No sections yet"
            description="Schedules belong to a section, so create one first."
            action={
              <LinkButton href="/sections" variant="primary">
                Create a section
              </LinkButton>
            }
          />
        </Card>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Schedules"
        description="Each schedule opens a present window, then a late window. A scan outside both records nothing."
        actions={
          <>
            {sections ? (
              <SectionPicker sections={sections} value={selectedId} onChange={select} />
            ) : null}
            <Button
              variant="primary"
              icon={<Plus aria-hidden className="size-4" />}
              onClick={() => setEditing("new")}
              disabled={!selectedId}
            >
              New schedule
            </Button>
          </>
        }
      />

      {schedules && schedules.length === 0 ? (
        <Card>
          <EmptyState
            icon={CalendarClock}
            title="No schedules for this section"
            description="Add one so the scanner knows when to accept attendance."
            action={
              <Button variant="primary" onClick={() => setEditing("new")}>
                New schedule
              </Button>
            }
          />
        </Card>
      ) : (
        <ul className="grid gap-3 lg:grid-cols-2">
          <AnimatePresence initial={false}>
            {(schedules ?? []).map((schedule, index) => (
              <m.li
                key={schedule.id}
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
                <ScheduleCard
                  schedule={schedule}
                  onEdit={() => setEditing(schedule)}
                  onDelete={() => setPendingDelete(schedule)}
                />
              </m.li>
            ))}
          </AnimatePresence>
        </ul>
      )}

      {selectedId ? (
        <ScheduleDialog
          target={editing}
          sectionId={selectedId}
          onClose={() => setEditing(null)}
          onSaved={(message) => {
            setEditing(null);
            toast.success(message);
          }}
        />
      ) : null}

      <ConfirmDialog
        open={pendingDelete !== null}
        title={`Remove ${pendingDelete?.title ?? "schedule"}?`}
        description="It stops accepting scans. Attendance already recorded against it is kept."
        confirmLabel="Remove schedule"
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}

function ScheduleCard({
  schedule,
  onEdit,
  onDelete,
}: {
  schedule: Schedule;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const state = windowStateAt(schedule, minutesOfDay());
  const open = state === "present" || state === "late";

  return (
    <Card className="flex h-full flex-col gap-4 p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="truncate text-base font-semibold tracking-tight text-ink">
            {schedule.title}
          </h2>
          {schedule.venue ? (
            <p className="mt-0.5 flex items-center gap-1.5 truncate text-sm text-muted">
              <MapPin aria-hidden className="size-3.5 shrink-0" />
              {schedule.venue}
            </p>
          ) : null}
        </div>
        <span
          className={cn(
            "shrink-0 rounded-full px-2.5 py-1 text-xs font-medium",
            open ? "bg-primary-soft text-primary" : "bg-canvas text-muted",
          )}
        >
          {describeWindowState(state)}
        </span>
      </div>

      <dl className="grid grid-cols-2 gap-3 text-sm">
        <div className="rounded-lg bg-present-soft px-3 py-2">
          <dt className="text-xs font-medium uppercase tracking-wide text-present">Present</dt>
          <dd className="mt-0.5 font-mono text-sm text-ink">
            {formatTime12(schedule.present.start)} – {formatTime12(schedule.present.end)}
          </dd>
        </div>
        <div className="rounded-lg bg-late-soft px-3 py-2">
          <dt className="text-xs font-medium uppercase tracking-wide text-late">Late</dt>
          <dd className="mt-0.5 font-mono text-sm text-ink">
            {formatTime12(schedule.late.start)} – {formatTime12(schedule.late.end)}
          </dd>
        </div>
      </dl>

      <div className="mt-auto flex gap-2">
        <Button size="sm" icon={<Pencil aria-hidden className="size-3.5" />} onClick={onEdit}>
          Edit
        </Button>
        <Button
          size="sm"
          variant="ghost"
          icon={<Trash2 aria-hidden className="size-3.5" />}
          onClick={onDelete}
        >
          Remove
        </Button>
      </div>
    </Card>
  );
}

interface FormState {
  title: string;
  venue: string;
  presentStart: string;
  presentEnd: string;
  lateStart: string;
  lateEnd: string;
}

const EMPTY_FORM: FormState = {
  title: "",
  venue: "",
  presentStart: "07:00",
  presentEnd: "07:30",
  lateStart: "07:30",
  lateEnd: "08:00",
};

function ScheduleDialog({
  target,
  sectionId,
  onClose,
  onSaved,
}: {
  target: Schedule | "new" | null;
  sectionId: string;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const isNew = target === "new";
  const schedule = target === "new" || target === null ? null : target;

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  const [lastTarget, setLastTarget] = useState<typeof target>(null);

  if (target !== lastTarget) {
    setLastTarget(target);
    setForm(
      schedule
        ? {
            title: schedule.title,
            venue: schedule.venue,
            presentStart: schedule.present.start,
            presentEnd: schedule.present.end,
            lateStart: schedule.late.start,
            lateEnd: schedule.late.end,
          }
        : EMPTY_FORM,
    );
    setError(undefined);
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => {
      const next = { ...current, [key]: value };
      // Keeping the late window butted against the present one is what people almost always
      // want; they can still pull it apart afterwards to leave a deliberate gap.
      if (key === "presentEnd") next.lateStart = value;
      return next;
    });
  }

  async function submit() {
    setSaving(true);
    setError(undefined);
    try {
      const payload = {
        sectionId,
        title: form.title,
        venue: form.venue,
        present: { start: form.presentStart, end: form.presentEnd },
        late: { start: form.lateStart, end: form.lateEnd },
      };
      if (isNew) {
        await createSchedule(payload);
        onSaved(`Created ${form.title.trim()}.`);
      } else if (schedule?.id) {
        await updateSchedule(schedule.id, payload);
        onSaved("Schedule updated.");
      }
    } catch (caught) {
      setError(errorMessage(caught, "Could not save that schedule."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      open={target !== null}
      onClose={onClose}
      size="lg"
      title={isNew ? "New schedule" : "Edit schedule"}
      description="Scans inside the present window count as present; scans inside the late window count as late."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={submit} disabled={saving}>
            {saving ? "Saving…" : isNew ? "Create schedule" : "Save changes"}
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
        <Field label="Title">
          {({ id }) => (
            <TextInput
              id={id}
              autoFocus
              maxLength={80}
              placeholder="Morning Assembly"
              value={form.title}
              onChange={(event) => set("title", event.target.value)}
            />
          )}
        </Field>
        <Field label="Venue" hint="Optional">
          {({ id }) => (
            <TextInput
              id={id}
              maxLength={80}
              placeholder="Quadrangle"
              value={form.venue}
              onChange={(event) => set("venue", event.target.value)}
            />
          )}
        </Field>

        <Field label="Present window opens">
          {({ id }) => (
            <TextInput
              id={id}
              type="time"
              value={form.presentStart}
              onChange={(event) => set("presentStart", event.target.value)}
            />
          )}
        </Field>
        <Field label="Present window closes">
          {({ id }) => (
            <TextInput
              id={id}
              type="time"
              value={form.presentEnd}
              onChange={(event) => set("presentEnd", event.target.value)}
            />
          )}
        </Field>
        <Field label="Late window opens">
          {({ id }) => (
            <TextInput
              id={id}
              type="time"
              value={form.lateStart}
              onChange={(event) => set("lateStart", event.target.value)}
            />
          )}
        </Field>
        <Field
          label="Late window closes"
          error={error}
          hint="Everyone not scanned by this time is marked absent"
        >
          {({ id, describedBy, invalid }) => (
            <TextInput
              id={id}
              type="time"
              aria-describedby={describedBy}
              aria-invalid={invalid}
              value={form.lateEnd}
              onChange={(event) => set("lateEnd", event.target.value)}
            />
          )}
        </Field>
      </form>
    </Dialog>
  );
}
