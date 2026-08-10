"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { Download, HardDrive, Trash2, Upload } from "lucide-react";
import { useRef, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { errorMessage, useToast } from "@/components/ui/Toast";
import { db } from "@/lib/db";
import { triggerDownload } from "@/lib/export/attendanceReport";
import {
  exportBackup,
  resetDatabase,
  restoreBackupFromJson,
  storageUsage,
} from "@/lib/services/backup";
import { seedDemoData } from "@/lib/services/seed";
import { today } from "@/domain/time";

/** A backup of a whole school is still small, but a hostile file need not be. */
const MAX_BACKUP_BYTES = 64 * 1024 * 1024;

export function DataView() {
  const toast = useToast();
  const fileInput = useRef<HTMLInputElement>(null);
  const [pendingRestore, setPendingRestore] = useState<File | null>(null);
  const [confirmReset, setConfirmReset] = useState(false);
  const [busy, setBusy] = useState(false);

  const counts = useLiveQuery(
    async () => {
      const database = db();
      const [sections, students, schedules, records, days] = await Promise.all([
        database.sections.count(),
        database.students.count(),
        database.schedules.count(),
        database.records.count(),
        database.schoolDays.count(),
      ]);
      return { sections, students, schedules, records, days };
    },
    [],
    undefined,
  );

  const usage = useLiveQuery(() => storageUsage(), [], undefined);

  async function handleBackup() {
    setBusy(true);
    try {
      const backup = await exportBackup();
      const blob = new Blob([JSON.stringify(backup, null, 2)], { type: "application/json" });
      triggerDownload(blob, `attendance-backup-${today()}.json`);
      toast.success("Backup downloaded.");
    } catch (error) {
      toast.error(errorMessage(error, "Could not create a backup."));
    } finally {
      setBusy(false);
    }
  }

  async function handleRestore() {
    const file = pendingRestore;
    setPendingRestore(null);
    if (!file) return;

    setBusy(true);
    try {
      if (file.size > MAX_BACKUP_BYTES) {
        toast.error("That file is too large to be an attendance backup.");
        return;
      }
      const result = await restoreBackupFromJson(await file.text());
      if (!result.ok) {
        toast.error(result.error);
        return;
      }
      toast.success(
        `Restored ${result.counts.students} students and ${result.counts.records} records.`,
      );
    } catch (error) {
      toast.error(errorMessage(error, "Could not read that file."));
    } finally {
      setBusy(false);
      if (fileInput.current) fileInput.current.value = "";
    }
  }

  async function handleReset() {
    setConfirmReset(false);
    setBusy(true);
    try {
      await resetDatabase();
      toast.success("All data erased.");
    } catch (error) {
      toast.error(errorMessage(error, "Could not erase the data."));
    } finally {
      setBusy(false);
    }
  }

  async function handleReseed() {
    setBusy(true);
    try {
      const seeded = await seedDemoData();
      toast[seeded ? "success" : "info"](
        seeded
          ? "Demo data loaded."
          : "Demo data is only loaded into an empty database. Erase everything first.",
      );
    } catch (error) {
      toast.error(errorMessage(error, "Could not load the demo data."));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Data"
        description="Every record lives in this browser. Back it up before switching devices or clearing site data."
      />

      <Card>
        <CardHeader title="What is stored" />
        <CardBody className="grid grid-cols-2 gap-4 sm:grid-cols-5">
          {[
            ["Sections", counts?.sections],
            ["Students", counts?.students],
            ["Schedules", counts?.schedules],
            ["Records", counts?.records],
            ["School days", counts?.days],
          ].map(([label, value]) => (
            <div key={String(label)}>
              <dt className="text-xs font-medium uppercase tracking-wide text-subtle">
                {label}
              </dt>
              <dd className="mt-0.5 text-xl font-semibold tabular text-ink">{value ?? "—"}</dd>
            </div>
          ))}
        </CardBody>
        {usage ? (
          <CardBody className="flex items-center gap-2 border-t border-border text-sm text-muted">
            <HardDrive aria-hidden className="size-4 shrink-0" />
            Using {formatBytes(usage.usedBytes)} of roughly {formatBytes(usage.quotaBytes)}{" "}
            available to this site.
          </CardBody>
        ) : null}
      </Card>

      <Card>
        <CardHeader
          title="Backup and restore"
          description="A backup is a plain JSON file. Restoring replaces everything currently stored."
        />
        <CardBody className="flex flex-wrap gap-2">
          <Button
            variant="primary"
            disabled={busy}
            icon={<Download aria-hidden className="size-4" />}
            onClick={handleBackup}
          >
            Download backup
          </Button>

          <Button
            disabled={busy}
            icon={<Upload aria-hidden className="size-4" />}
            onClick={() => fileInput.current?.click()}
          >
            Restore from file
          </Button>
          <input
            ref={fileInput}
            type="file"
            accept="application/json,.json"
            className="sr-only"
            aria-label="Backup file"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) setPendingRestore(file);
            }}
          />

          <Button disabled={busy} onClick={handleReseed}>
            Load demo data
          </Button>
        </CardBody>
      </Card>

      <Card className="border-danger/30">
        <CardHeader
          title="Erase everything"
          description="Removes all sections, students, schedules, and attendance records from this browser. This cannot be undone."
        />
        <CardBody>
          <Button
            variant="danger"
            disabled={busy}
            icon={<Trash2 aria-hidden className="size-4" />}
            onClick={() => setConfirmReset(true)}
          >
            Erase all data
          </Button>
        </CardBody>
      </Card>

      <ConfirmDialog
        open={pendingRestore !== null}
        title="Replace all data with this backup?"
        description="Everything currently stored in this browser is discarded and replaced by the contents of the file. Download a backup first if you might want it back."
        confirmLabel="Restore backup"
        onConfirm={handleRestore}
        onCancel={() => {
          setPendingRestore(null);
          if (fileInput.current) fileInput.current.value = "";
        }}
      />

      <ConfirmDialog
        open={confirmReset}
        title="Erase all attendance data?"
        description="Every section, student, schedule, and attendance record is deleted from this browser. There is no undo."
        confirmLabel="Erase everything"
        onConfirm={handleReset}
        onCancel={() => setConfirmReset(false)}
      />
    </div>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KB", "MB", "GB"];
  let value = bytes / 1024;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value.toFixed(value < 10 ? 1 : 0)} ${units[unit]}`;
}
