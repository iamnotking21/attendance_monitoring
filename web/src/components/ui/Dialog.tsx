"use client";

import { X } from "lucide-react";
import { useEffect, useRef, type ReactNode } from "react";

import { cn } from "@/lib/cn";

/**
 * Built on the native `<dialog>` element, which supplies the focus trap, the inert background,
 * the Escape handler, and the top-layer stacking for free. Hand-rolling those in JavaScript is
 * where most custom modals quietly break for keyboard and screen-reader users.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = "md",
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  size?: "md" | "lg";
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    if (open && !element.open) element.showModal();
    if (!open && element.open) element.close();
  }, [open]);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const handleCancel = (event: Event) => {
      event.preventDefault();
      onClose();
    };
    element.addEventListener("cancel", handleCancel);
    return () => element.removeEventListener("cancel", handleCancel);
  }, [onClose]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="dialog-title"
      className={cn(
        "m-auto w-[calc(100vw-2rem)] rounded-xl border border-border bg-surface p-0 text-ink shadow-pop",
        "backdrop:bg-black/45 backdrop:backdrop-blur-[2px]",
        "open:animate-[dialog-in_200ms_var(--ease-out-soft)]",
        size === "lg" ? "max-w-2xl" : "max-w-md",
      )}
      onClick={(event) => {
        // A click that lands on the dialog element itself is a click on the backdrop; the
        // panel inside stops its own clicks from reaching here.
        if (event.target === ref.current) onClose();
      }}
    >
      <div className="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
        <div className="min-w-0">
          <h2 id="dialog-title" className="text-base font-semibold tracking-tight">
            {title}
          </h2>
          {description ? <p className="mt-1 text-sm text-muted">{description}</p> : null}
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close dialog"
          className="-mr-2 -mt-2 grid size-11 place-items-center rounded-lg text-muted transition-colors hover:bg-primary-soft hover:text-ink"
        >
          <X aria-hidden className="size-4" />
        </button>
      </div>

      <div className="max-h-[70vh] overflow-y-auto px-5 py-4">{children}</div>

      {footer ? (
        <div className="flex flex-wrap justify-end gap-2 border-t border-border px-5 py-3.5">
          {footer}
        </div>
      ) : null}

      <style>{`
        @keyframes dialog-in {
          from { opacity: 0; transform: translateY(8px) scale(0.98); }
          to   { opacity: 1; transform: none; }
        }
      `}</style>
    </dialog>
  );
}
