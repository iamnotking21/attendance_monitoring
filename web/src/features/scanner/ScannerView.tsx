"use client";

import { CameraOff, Keyboard, ScanLine } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { TextInput } from "@/components/ui/Field";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { errorMessage, useToast } from "@/components/ui/Toast";
import { fullName, type AttendanceStatus } from "@/domain/model";
import { recordScan, type ScanResult } from "@/lib/services/attendance";
import { cn } from "@/lib/cn";

interface FeedEntry {
  id: number;
  name: string;
  detail: string;
  status?: AttendanceStatus;
  tone: "good" | "warn" | "bad";
  at: string;
}

/** A badge held to the lens decodes many times a second; one accepted scan per badge is enough. */
const REPEAT_SUPPRESSION_MS = 2500;

export function ScannerView() {
  const toast = useToast();
  const videoRef = useRef<HTMLVideoElement>(null);
  const scannerRef = useRef<{ stop: () => void; destroy: () => void } | null>(null);
  const lastPayload = useRef<{ value: string; at: number }>({ value: "", at: 0 });

  const [cameraState, setCameraState] = useState<"idle" | "starting" | "running" | "failed">(
    "idle",
  );
  const [cameraError, setCameraError] = useState<string>();
  const [feed, setFeed] = useState<FeedEntry[]>([]);
  const [manual, setManual] = useState("");

  const handlePayload = useCallback(
    async (payload: string) => {
      const now = Date.now();
      if (
        payload === lastPayload.current.value &&
        now - lastPayload.current.at < REPEAT_SUPPRESSION_MS
      ) {
        return;
      }
      lastPayload.current = { value: payload, at: now };

      try {
        const result = await recordScan(payload);
        setFeed((current) => [toFeedEntry(result, payload), ...current].slice(0, 25));
      } catch (error) {
        toast.error(errorMessage(error, "Could not record that scan."));
      }
    },
    [toast],
  );

  const startCamera = useCallback(async () => {
    if (!videoRef.current || scannerRef.current) return;
    setCameraState("starting");
    setCameraError(undefined);

    try {
      // ~45 kB of decoder plus a worker, pulled in only when someone actually opens the camera.
      const { default: QrScanner } = await import("qr-scanner");

      if (!(await QrScanner.hasCamera())) {
        setCameraState("failed");
        setCameraError("No camera was found on this device.");
        return;
      }

      const scanner = new QrScanner(
        videoRef.current,
        (result) => void handlePayload(result.data),
        {
          returnDetailedScanResult: true,
          highlightScanRegion: true,
          highlightCodeOutline: true,
          preferredCamera: "environment",
          maxScansPerSecond: 5,
        },
      );

      await scanner.start();
      scannerRef.current = scanner;
      setCameraState("running");
    } catch (error) {
      setCameraState("failed");
      setCameraError(
        error instanceof Error && error.name === "NotAllowedError"
          ? "Camera access was blocked. Allow it in your browser's site settings, or type student numbers below."
          : errorMessage(error, "The camera could not be started."),
      );
    }
  }, [handlePayload]);

  useEffect(() => {
    return () => {
      scannerRef.current?.stop();
      scannerRef.current?.destroy();
      scannerRef.current = null;
    };
  }, []);

  const summary = useMemo(() => {
    const counts = { present: 0, late: 0, other: 0 };
    for (const entry of feed) {
      if (entry.status === "present") counts.present += 1;
      else if (entry.status === "late") counts.late += 1;
      else counts.other += 1;
    }
    return counts;
  }, [feed]);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Scan"
        description="Hold a student's QR code to the camera. The open window decides whether it counts as present or late."
      />

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <Card className="overflow-hidden">
          <div className="relative aspect-[4/3] w-full bg-black sm:aspect-video">
            <video
              ref={videoRef}
              className={cn(
                "size-full object-cover transition-opacity duration-300",
                cameraState === "running" ? "opacity-100" : "opacity-0",
              )}
              muted
              playsInline
              aria-label="Camera preview"
            />

            {cameraState !== "running" ? (
              <div className="absolute inset-0 grid place-items-center px-6 text-center">
                {cameraState === "failed" ? (
                  <div className="flex flex-col items-center gap-3">
                    <CameraOff aria-hidden className="size-8 text-white/70" />
                    <p className="max-w-sm text-sm text-white/80">{cameraError}</p>
                    <Button onClick={() => void startCamera()}>Try again</Button>
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-3">
                    <ScanLine aria-hidden className="size-8 text-white/70" />
                    <p className="max-w-sm text-sm text-white/80">
                      The camera stays off until you start it, and the video never leaves this
                      device.
                    </p>
                    <Button
                      variant="primary"
                      onClick={() => void startCamera()}
                      disabled={cameraState === "starting"}
                    >
                      {cameraState === "starting" ? "Starting…" : "Start camera"}
                    </Button>
                  </div>
                )}
              </div>
            ) : null}
          </div>

          <CardBody className="flex flex-wrap items-center gap-x-5 gap-y-2 border-t border-border text-sm">
            <span className="text-muted">
              This session:{" "}
              <strong className="text-present tabular">{summary.present}</strong> present,{" "}
              <strong className="text-late tabular">{summary.late}</strong> late,{" "}
              <strong className="text-muted tabular">{summary.other}</strong> not recorded
            </span>
            {cameraState === "running" ? (
              <Button
                size="sm"
                className="ml-auto"
                onClick={() => {
                  scannerRef.current?.stop();
                  scannerRef.current?.destroy();
                  scannerRef.current = null;
                  setCameraState("idle");
                }}
              >
                Stop camera
              </Button>
            ) : null}
          </CardBody>
        </Card>

        <div className="flex flex-col gap-4">
          <Card>
            <CardHeader
              title="Type a student number"
              description="For a damaged badge, or a device with no camera."
            />
            <CardBody>
              <form
                className="flex gap-2"
                onSubmit={(event) => {
                  event.preventDefault();
                  const value = manual.trim();
                  if (!value) return;
                  // Bypasses repeat suppression: typing it twice is a deliberate act.
                  lastPayload.current = { value: "", at: 0 };
                  void handlePayload(value);
                  setManual("");
                }}
              >
                <TextInput
                  value={manual}
                  onChange={(event) => setManual(event.target.value)}
                  placeholder="2024-1001"
                  aria-label="Student number"
                  autoComplete="off"
                  maxLength={32}
                  className="font-mono"
                />
                <Button
                  type="submit"
                  variant="primary"
                  icon={<Keyboard aria-hidden className="size-4" />}
                >
                  Record
                </Button>
              </form>
            </CardBody>
          </Card>

          <Card className="min-h-48">
            <CardHeader title="Recent scans" />
            <ul className="divide-y divide-border">
              <AnimatePresence initial={false}>
                {feed.map((entry) => (
                  <motion.li
                    key={entry.id}
                    layout
                    initial={{ opacity: 0, x: -8 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
                    className="flex items-center justify-between gap-3 px-4 py-2.5 sm:px-5"
                  >
                    <div className="min-w-0">
                      <p
                        className={cn(
                          "truncate text-sm font-medium",
                          entry.tone === "bad" ? "text-danger" : "text-ink",
                        )}
                      >
                        {entry.name}
                      </p>
                      <p className="truncate text-xs text-subtle">
                        {entry.at} · {entry.detail}
                      </p>
                    </div>
                    {entry.status ? <StatusBadge status={entry.status} /> : null}
                  </motion.li>
                ))}
              </AnimatePresence>
            </ul>
            {feed.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm text-muted">
                Scans appear here as they are recorded.
              </p>
            ) : null}
          </Card>
        </div>
      </div>
    </div>
  );
}

function toFeedEntry(result: ScanResult, payload: string): FeedEntry {
  const at = new Date().toLocaleTimeString(undefined, {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
  const id = Date.now() + Math.random();

  switch (result.kind) {
    case "recorded": {
      const status = result.records[0]?.status;
      return {
        id,
        at,
        name: fullName(result.student),
        detail: result.records.map((record) => record.scheduleTitle).join(", "),
        status,
        tone: status === "late" ? "warn" : "good",
      };
    }
    case "duplicate":
      return {
        id,
        at,
        name: fullName(result.student),
        detail: "Already recorded today",
        tone: "warn",
      };
    case "closed":
      return {
        id,
        at,
        name: fullName(result.student),
        detail: "No attendance window is open right now",
        tone: "warn",
      };
    case "unknown":
      return {
        id,
        at,
        name: "Unknown student",
        detail: `${result.studentNumber} is not on any roster`,
        tone: "bad",
      };
    case "malformed":
      return { id, at, name: "Unrecognised code", detail: result.reason, tone: "bad" };
  }

  return { id, at, name: "Unrecognised code", detail: payload.slice(0, 24), tone: "bad" };
}
