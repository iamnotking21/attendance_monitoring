"use client";

import { Download } from "lucide-react";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/Button";
import { Dialog } from "@/components/ui/Dialog";
import { fullName, type Student } from "@/domain/model";
import { toSafeFilename } from "@/domain/spreadsheet";

/**
 * The QR encodes the student number and nothing else — no name, no section, no date of birth.
 * A badge gets photographed, dropped, and left on desks; the less it carries, the less a
 * stranger learns from finding one.
 */
export function QrCodeDialog({
  student,
  onClose,
}: {
  student: Student | null;
  onClose: () => void;
}) {
  // Tagged with the student it belongs to, so a previously rendered code can never linger on
  // screen next to a different student's name while the new one is still encoding.
  const [encoded, setEncoded] = useState<{ studentNumber: string; url: string } | null>(null);
  const [error, setError] = useState<string>();

  const dataUrl =
    student && encoded?.studentNumber === student.studentNumber ? encoded.url : undefined;

  useEffect(() => {
    if (!student) return;

    let cancelled = false;

    void (async () => {
      try {
        // Loaded on demand: the encoder is ~40 kB and only matters on this one dialog.
        const { toDataURL } = await import("qrcode");
        const url = await toDataURL(student.studentNumber, {
          width: 512,
          margin: 2,
          errorCorrectionLevel: "M",
          color: { dark: "#10131c", light: "#ffffff" },
        });
        if (cancelled) return;
        setError(undefined);
        setEncoded({ studentNumber: student.studentNumber, url });
      } catch {
        if (!cancelled) setError("Could not generate that QR code.");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [student]);

  function download() {
    if (!dataUrl || !student) return;
    const anchor = document.createElement("a");
    anchor.href = dataUrl;
    anchor.download = `${toSafeFilename(student.studentNumber, "student")}-qr.png`;
    anchor.click();
  }

  return (
    <Dialog
      open={student !== null}
      onClose={onClose}
      title="Student QR code"
      description={student ? fullName(student) : undefined}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button
            variant="primary"
            onClick={download}
            disabled={!dataUrl}
            icon={<Download aria-hidden className="size-4" />}
          >
            Download PNG
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3">
        <div className="grid size-56 place-items-center rounded-xl border border-border bg-white p-3">
          {error ? (
            <p className="px-4 text-center text-sm text-danger">{error}</p>
          ) : dataUrl ? (
            /* eslint-disable-next-line @next/next/no-img-element -- a data: URL generated in
               the browser has no remote origin for next/image to optimise. */
            <img
              src={dataUrl}
              alt={`QR code encoding student number ${student?.studentNumber ?? ""}`}
              className="size-full object-contain"
              width={512}
              height={512}
            />
          ) : (
            <div className="size-full animate-pulse rounded-lg bg-border" />
          )}
        </div>
        <p className="font-mono text-sm text-muted">{student?.studentNumber}</p>
      </div>
    </Dialog>
  );
}
