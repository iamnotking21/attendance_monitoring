import type { ButtonHTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

const VARIANTS: Record<Variant, string> = {
  primary:
    "bg-primary text-on-primary hover:bg-primary-hover shadow-sm disabled:hover:bg-primary",
  secondary:
    "bg-surface text-ink border border-border hover:border-border-strong hover:bg-raised",
  ghost: "text-muted hover:text-ink hover:bg-primary-soft",
  danger: "bg-danger text-white hover:opacity-90",
};

const SIZES: Record<Size, string> = {
  // 44px minimum height on every size: below that a touch target is unreliable on a phone.
  sm: "h-11 px-3 text-sm gap-1.5 sm:h-9",
  md: "h-11 px-4 text-sm gap-2",
  lg: "h-12 px-6 text-base gap-2",
};

const BASE =
  "inline-flex items-center justify-center rounded-lg font-medium " +
  "transition-[background-color,border-color,color,transform] duration-150 ease-out " +
  "active:scale-[0.98] disabled:pointer-events-none disabled:opacity-50";

/** Shared with `LinkButton`, so a navigating control and an acting control look identical. */
export function buttonClasses(variant: Variant = "secondary", size: Size = "md"): string {
  return cn(BASE, VARIANTS[variant], SIZES[size]);
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  icon?: ReactNode;
}

export function Button({
  variant = "secondary",
  size = "md",
  icon,
  className,
  children,
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(buttonClasses(variant, size), className)}
      {...props}
    >
      {icon}
      {children}
    </button>
  );
}
