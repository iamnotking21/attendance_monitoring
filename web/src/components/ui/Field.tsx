"use client";

import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

const CONTROL =
  "h-11 w-full rounded-lg border border-border bg-surface px-3 text-sm text-ink " +
  "placeholder:text-subtle transition-colors duration-150 " +
  "hover:border-border-strong focus:border-primary focus:outline-none " +
  "aria-[invalid=true]:border-danger";

export function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string;
  error?: string;
  hint?: string;
  children: (props: { id: string; describedBy?: string; invalid: boolean }) => ReactNode;
}) {
  const id = useId();
  const messageId = `${id}-message`;
  const message = error ?? hint;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-ink">
        {label}
      </label>
      {children({
        id,
        describedBy: message ? messageId : undefined,
        invalid: Boolean(error),
      })}
      {message ? (
        <p
          id={messageId}
          // Errors announce themselves; hints are read only when the field is focused.
          role={error ? "alert" : undefined}
          className={cn("text-xs", error ? "text-danger" : "text-muted")}
        >
          {message}
        </p>
      ) : null}
    </div>
  );
}

export function TextInput({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(CONTROL, className)} {...props} />;
}

export function SelectInput({
  className,
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cn(CONTROL, "pr-8", className)} {...props}>
      {children}
    </select>
  );
}
