"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { Users } from "lucide-react";
import { AnimatePresence, m } from "motion/react";
import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { LinkButton } from "@/components/ui/LinkButton";
import { TextInput } from "@/components/ui/Field";
import { SectionPicker } from "@/components/ui/SectionPicker";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { windowStateAt } from "@/domain/attendance";
import { fullName, type AttendanceStatus } from "@/domain/model";
import {
  attendanceRate,
  buildDashboard,
  entriesByStatus,
  formatRate,
} from "@/domain/reporting";
import { formatDateLong, formatTime12, minutesOfDay, today } from "@/domain/time";
import { listRecordsBySectionAndDate } from "@/lib/repositories/records";
import { listSchedulesBySection } from "@/lib/repositories/schedules";
import { listStudentsBySection } from "@/lib/repositories/students";
import { describeWindowState } from "@/lib/services/attendance";
import { useSectionSelection } from "@/features/shared/useSectionSelection";
import { cn } from "@/lib/cn";

type Tab = AttendanceStatus | "unrecorded";

const TABS: { key: Tab; label: string }[] = [
  { key: "present", label: "Present" },
  { key: "late", label: "Late" },
  { key: "absent", label: "Absent" },
  { key: "unrecorded", label: "Not scanned" },
];

export function DashboardView() {
  const { sections, selected, selectedId, select, loading } = useSectionSelection();
  const [date, setDate] = useState(() => today());
  const [tab, setTab] = useState<Tab>("present");

  const students = useLiveQuery(
    () => (selectedId ? listStudentsBySection(selectedId) : Promise.resolve([])),
    [selectedId],
    undefined,
  );
  const records = useLiveQuery(
    () => (selectedId ? listRecordsBySectionAndDate(selectedId, date) : Promise.resolve([])),
    [selectedId, date],
    undefined,
  );
  const schedules = useLiveQuery(
    () => (selectedId ? listSchedulesBySection(selectedId) : Promise.resolve([])),
    [selectedId],
    undefined,
  );

  const breakdown = useMemo(
    () => buildDashboard(students ?? [], records ?? [], date),
    [students, records, date],
  );

  const rate = attendanceRate(breakdown.counts);
  const isToday = date === today();

  if (loading) return <DashboardSkeleton />;

  if (!sections || sections.length === 0) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title="Today" />
        <Card>
          <EmptyState
            icon={Users}
            title="No sections yet"
            description="Create a section and add students before attendance can be recorded."
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
        title={isToday ? "Today" : formatDateLong(date)}
        description={
          selected ? `${selected.name} · ${formatDateLong(date)}` : formatDateLong(date)
        }
        actions={
          <>
            <label className="flex items-center gap-2">
              <span className="sr-only">Date</span>
              <TextInput
                type="date"
                value={date}
                max={today()}
                onChange={(event) => setDate(event.target.value || today())}
                className="w-auto"
                aria-label="Date"
              />
            </label>
            <SectionPicker sections={sections} value={selectedId} onChange={select} />
          </>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <StatTile label="Present" value={breakdown.counts.present} tone="present" />
        <StatTile label="Late" value={breakdown.counts.late} tone="late" />
        <StatTile label="Absent" value={breakdown.counts.absent} tone="absent" />
        <StatTile label="Attendance rate" value={formatRate(rate)} tone="neutral" />
      </div>

      {isToday && schedules && schedules.length > 0 ? (
        <ScheduleStrip schedules={schedules} />
      ) : null}

      <Card>
        <CardHeader
          title="Roster"
          description={`${students?.length ?? 0} active students`}
          action={
            <div
              role="tablist"
              aria-label="Attendance status"
              className="scroll-x flex gap-1 rounded-lg bg-canvas p-1"
            >
              {TABS.map((entry) => {
                const active = tab === entry.key;
                return (
                  <button
                    key={entry.key}
                    role="tab"
                    aria-selected={active}
                    onClick={() => setTab(entry.key)}
                    className={cn(
                      "relative min-h-11 whitespace-nowrap rounded-md px-3 text-sm font-medium transition-colors duration-150 sm:min-h-9",
                      active ? "text-ink" : "text-muted hover:text-ink",
                    )}
                  >
                    {active ? (
                      <m.span
                        layoutId="roster-tab"
                        className="absolute inset-0 rounded-md bg-surface shadow-sm"
                        transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
                      />
                    ) : null}
                    <span className="relative">
                      {entry.label}
                      <span className="ml-1.5 text-xs text-subtle tabular">
                        {countFor(breakdown, entry.key)}
                      </span>
                    </span>
                  </button>
                );
              })}
            </div>
          }
        />
        <CardBody className="p-0 sm:p-0">
          <RosterPanel tab={tab} breakdown={breakdown} />
        </CardBody>
      </Card>
    </div>
  );
}

function countFor(
  breakdown: ReturnType<typeof buildDashboard>,
  tab: Tab,
): number {
  return tab === "unrecorded" ? breakdown.unaccountedFor.length : breakdown.counts[tab];
}

function RosterPanel({
  tab,
  breakdown,
}: {
  tab: Tab;
  breakdown: ReturnType<typeof buildDashboard>;
}) {
  const rows =
    tab === "unrecorded"
      ? breakdown.unaccountedFor.map((student) => ({
          key: `${student.studentNumber}-unrecorded`,
          name: fullName(student),
          gender: student.gender,
          studentNumber: student.studentNumber,
          status: undefined,
          detail: "Not scanned yet",
        }))
      : entriesByStatus(breakdown, tab).map((entry) => ({
          key: `${entry.student.studentNumber}-${entry.scheduleTitle}`,
          name: entry.displayName,
          gender: entry.student.gender,
          studentNumber: entry.student.studentNumber,
          status: entry.status,
          detail: entry.scheduleTitle,
        }));

  if (rows.length === 0) {
    return (
      <p className="px-5 py-10 text-center text-sm text-muted">
        Nobody in this group for the selected day.
      </p>
    );
  }

  return (
    <div className="grid gap-x-6 sm:grid-cols-2">
      {(["male", "female"] as const).map((gender) => {
        const group = rows.filter((row) => row.gender === gender);
        return (
          <section key={gender} className="min-w-0">
            <h3 className="px-4 pt-4 text-xs font-semibold uppercase tracking-wide text-subtle sm:px-5">
              {gender === "male" ? "Boys" : "Girls"}
              <span className="ml-1.5 tabular">({group.length})</span>
            </h3>
            <ul className="mt-1 divide-y divide-border">
              <AnimatePresence initial={false}>
                {group.map((row, index) => (
                  <m.li
                    key={row.key}
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0 }}
                    // Capped stagger: a 40-student roster must not take four seconds to appear.
                    transition={{
                      duration: 0.2,
                      delay: Math.min(index * 0.02, 0.3),
                      ease: [0.22, 1, 0.36, 1],
                    }}
                    className="flex items-center justify-between gap-3 px-4 py-2.5 sm:px-5"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-ink">{row.name}</p>
                      <p className="font-mono text-xs text-subtle">
                        <span className="block truncate sm:inline">{row.studentNumber}</span>
                        <span aria-hidden className="hidden sm:inline"> · </span>
                        <span className="block truncate sm:inline">{row.detail}</span>
                      </p>
                    </div>
                    {row.status ? <StatusBadge status={row.status} /> : null}
                  </m.li>
                ))}
              </AnimatePresence>
            </ul>
            {group.length === 0 ? (
              <p className="px-4 pb-4 text-sm text-subtle sm:px-5">None</p>
            ) : null}
          </section>
        );
      })}
    </div>
  );
}

function ScheduleStrip({
  schedules,
}: {
  schedules: NonNullable<Awaited<ReturnType<typeof listSchedulesBySection>>>;
}) {
  const atMinutes = minutesOfDay();

  return (
    <Card>
      <CardHeader title="Today's windows" description="When each schedule accepts scans" />
      <CardBody className="scroll-x flex gap-3 px-4 sm:px-5">
        {schedules.map((schedule) => {
          const state = windowStateAt(schedule, atMinutes);
          const open = state === "present" || state === "late";
          return (
            <div
              key={schedule.id}
              className={cn(
                "min-w-56 shrink-0 rounded-lg border p-3",
                open ? "border-primary/40 bg-primary-soft" : "border-border bg-canvas",
              )}
            >
              <div className="flex items-center gap-2">
                {open ? (
                  <span className="relative flex size-2">
                    <span className="absolute inline-flex size-full animate-ping rounded-full bg-primary opacity-60" />
                    <span className="relative inline-flex size-2 rounded-full bg-primary" />
                  </span>
                ) : (
                  <span className="size-2 rounded-full bg-border-strong" />
                )}
                <p className="truncate text-sm font-medium text-ink">{schedule.title}</p>
              </div>
              <p className="mt-1 text-xs text-muted">{describeWindowState(state)}</p>
              <p className="mt-1.5 font-mono text-xs text-subtle">
                {formatTime12(schedule.present.start)} – {formatTime12(schedule.present.end)}
                {" · late to "}
                {formatTime12(schedule.late.end)}
              </p>
            </div>
          );
        })}
      </CardBody>
    </Card>
  );
}

function StatTile({
  label,
  value,
  tone,
}: {
  label: string;
  value: number | string;
  tone: "present" | "late" | "absent" | "neutral";
}) {
  const toneClass = {
    present: "text-present",
    late: "text-late",
    absent: "text-absent",
    neutral: "text-primary",
  }[tone];

  return (
    <Card className="px-4 py-3.5">
      <p className="text-xs font-medium uppercase tracking-wide text-subtle">{label}</p>
      <m.p
        key={String(value)}
        initial={{ opacity: 0, y: 4 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
        className={cn("mt-1 text-2xl font-semibold tabular sm:text-3xl", toneClass)}
      >
        {value}
      </m.p>
    </Card>
  );
}

function DashboardSkeleton() {
  return (
    <div className="flex flex-col gap-6" aria-busy="true" aria-label="Loading dashboard">
      <div className="h-8 w-40 animate-pulse rounded-lg bg-border" />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {[0, 1, 2, 3].map((index) => (
          <div key={index} className="h-20 animate-pulse rounded-xl bg-border" />
        ))}
      </div>
      <div className="h-64 animate-pulse rounded-xl bg-border" />
    </div>
  );
}
