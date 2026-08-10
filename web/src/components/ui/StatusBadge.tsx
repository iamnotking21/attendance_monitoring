import { CircleCheck, CircleMinus, Clock3 } from "lucide-react";

import type { AttendanceStatus } from "@/domain/model";
import { cn } from "@/lib/cn";

/**
 * Status is carried by an icon and a word, not by colour alone — roughly one man in twelve
 * cannot reliably separate the green and the red on their own.
 */
const STYLES: Record<AttendanceStatus, { label: string; className: string }> = {
  present: { label: "Present", className: "bg-present-soft text-present" },
  late: { label: "Late", className: "bg-late-soft text-late" },
  absent: { label: "Absent", className: "bg-absent-soft text-absent" },
};

const ICONS: Record<AttendanceStatus, typeof CircleCheck> = {
  present: CircleCheck,
  late: Clock3,
  absent: CircleMinus,
};

export function StatusBadge({
  status,
  className,
}: {
  status: AttendanceStatus;
  className?: string;
}) {
  const Icon = ICONS[status];
  const { label, className: tone } = STYLES[status];

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium",
        tone,
        className,
      )}
    >
      <Icon aria-hidden className="size-3.5" />
      {label}
    </span>
  );
}

export function statusLabel(status: AttendanceStatus): string {
  return STYLES[status].label;
}
