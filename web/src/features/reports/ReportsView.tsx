"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { FileSpreadsheet, ScrollText } from "lucide-react";
import { m } from "motion/react";
import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { TextInput } from "@/components/ui/Field";
import { LinkButton } from "@/components/ui/LinkButton";
import { SectionPicker } from "@/components/ui/SectionPicker";
import { errorMessage, useToast } from "@/components/ui/Toast";
import { formatRate, summariseStudents } from "@/domain/reporting";
import { addDays, formatDateShort, monthRange, today } from "@/domain/time";
import { listRecordsBySectionBetween } from "@/lib/repositories/records";
import { listStudentsBySection } from "@/lib/repositories/students";
import {
  buildCsv,
  csvBlob,
  downloadXlsx,
  reportFilename,
  triggerDownload,
} from "@/lib/export/attendanceReport";
import { useSectionSelection } from "@/features/shared/useSectionSelection";
import { cn } from "@/lib/cn";

export function ReportsView() {
  const toast = useToast();
  const { sections, selected, selectedId, select, loading } = useSectionSelection();

  const [range, setRange] = useState(() => monthRange(today()));
  const [exporting, setExporting] = useState<"csv" | "xlsx" | null>(null);

  const students = useLiveQuery(
    () => (selectedId ? listStudentsBySection(selectedId) : Promise.resolve([])),
    [selectedId],
    undefined,
  );
  const records = useLiveQuery(
    () =>
      selectedId
        ? listRecordsBySectionBetween(selectedId, range.start, range.end)
        : Promise.resolve([]),
    [selectedId, range.start, range.end],
    undefined,
  );

  const summaries = useMemo(
    () => summariseStudents(students ?? [], records ?? [], range),
    [students, records, range],
  );

  const context = useMemo(
    () => ({ sectionName: selected?.name ?? "section", range, summaries }),
    [selected, range, summaries],
  );

  const rangeIsValid = range.start <= range.end;

  async function exportCsv() {
    setExporting("csv");
    try {
      triggerDownload(csvBlob(buildCsv(context)), reportFilename(context, "csv"));
      toast.success("CSV downloaded.");
    } catch (error) {
      toast.error(errorMessage(error, "Could not build that CSV."));
    } finally {
      setExporting(null);
    }
  }

  async function exportXlsx() {
    setExporting("xlsx");
    try {
      await downloadXlsx(context);
      toast.success("Excel file downloaded.");
    } catch (error) {
      toast.error(errorMessage(error, "Could not build that spreadsheet."));
    } finally {
      setExporting(null);
    }
  }

  if (!loading && (!sections || sections.length === 0)) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title="Reports" />
        <Card>
          <EmptyState
            icon={ScrollText}
            title="Nothing to report on yet"
            description="Create a section and record some attendance first."
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
        title="Reports"
        description="Attendance per student over a date range. Late counts as attending — it is a punctuality problem, not an absence."
        actions={
          sections ? (
            <SectionPicker sections={sections} value={selectedId} onChange={select} />
          ) : null
        }
      />

      <Card>
        <CardHeader
          title="Date range"
          action={
            <div className="flex flex-wrap gap-2">
              {[
                { label: "This month", value: () => monthRange(today()) },
                {
                  label: "Last 7 days",
                  value: () => ({ start: addDays(today(), -6), end: today() }),
                },
                {
                  label: "Last 30 days",
                  value: () => ({ start: addDays(today(), -29), end: today() }),
                },
              ].map((preset) => (
                <Button key={preset.label} size="sm" onClick={() => setRange(preset.value())}>
                  {preset.label}
                </Button>
              ))}
            </div>
          }
        />
        <CardBody className="flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-ink">From</span>
            <TextInput
              type="date"
              value={range.start}
              max={range.end}
              onChange={(event) =>
                setRange((current) => ({ ...current, start: event.target.value }))
              }
              className="w-auto"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-ink">To</span>
            <TextInput
              type="date"
              value={range.end}
              min={range.start}
              onChange={(event) =>
                setRange((current) => ({ ...current, end: event.target.value }))
              }
              className="w-auto"
            />
          </label>

          <div className="ml-auto flex flex-wrap gap-2">
            <Button
              onClick={exportCsv}
              disabled={!rangeIsValid || exporting !== null || summaries.length === 0}
            >
              {exporting === "csv" ? "Preparing…" : "Export CSV"}
            </Button>
            <Button
              variant="primary"
              icon={<FileSpreadsheet aria-hidden className="size-4" />}
              onClick={exportXlsx}
              disabled={!rangeIsValid || exporting !== null || summaries.length === 0}
            >
              {exporting === "xlsx" ? "Preparing…" : "Export Excel"}
            </Button>
          </div>
        </CardBody>
      </Card>

      {!rangeIsValid ? (
        <p role="alert" className="text-sm text-danger">
          The start date must fall on or before the end date.
        </p>
      ) : null}

      <Card>
        <CardHeader
          title={selected?.name ?? "Section"}
          description={`${formatDateShort(range.start)} – ${formatDateShort(range.end)} · ${summaries.length} students`}
        />
        {summaries.length === 0 ? (
          <EmptyState
            icon={ScrollText}
            title="No students in this section"
            description="Add students to the section to see their attendance here."
          />
        ) : (
          <div className="scroll-x">
            <table className="w-full min-w-[42rem] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-subtle">
                  <th scope="col" className="px-4 py-2.5 font-medium sm:px-5">
                    Student
                  </th>
                  <th scope="col" className="px-3 py-2.5 text-right font-medium">
                    Present
                  </th>
                  <th scope="col" className="px-3 py-2.5 text-right font-medium">
                    Late
                  </th>
                  <th scope="col" className="px-3 py-2.5 text-right font-medium">
                    Absent
                  </th>
                  <th scope="col" className="px-4 py-2.5 text-right font-medium sm:px-5">
                    Rate
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {summaries.map((summary, index) => (
                  <m.tr
                    key={summary.student.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ duration: 0.2, delay: Math.min(index * 0.015, 0.3) }}
                  >
                    <th scope="row" className="px-4 py-2.5 text-left font-normal sm:px-5">
                      <span className="block truncate font-medium text-ink">
                        {summary.displayName}
                      </span>
                      <span className="block truncate font-mono text-xs text-subtle">
                        {summary.student.studentNumber}
                      </span>
                    </th>
                    <td className="px-3 py-2.5 text-right tabular text-present">
                      {summary.counts.present}
                    </td>
                    <td className="px-3 py-2.5 text-right tabular text-late">
                      {summary.counts.late}
                    </td>
                    <td className="px-3 py-2.5 text-right tabular text-absent">
                      {summary.counts.absent}
                    </td>
                    <td
                      className={cn(
                        "px-4 py-2.5 text-right font-medium tabular sm:px-5",
                        summary.sessions === 0
                          ? "text-subtle"
                          : summary.rate >= 0.9
                            ? "text-present"
                            : summary.rate >= 0.75
                              ? "text-late"
                              : "text-absent",
                      )}
                    >
                      {summary.sessions === 0 ? "—" : formatRate(summary.rate)}
                    </td>
                  </m.tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
