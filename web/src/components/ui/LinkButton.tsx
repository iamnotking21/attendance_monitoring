import Link from "next/link";
import type { ComponentProps, ReactNode } from "react";

import { buttonClasses } from "@/components/ui/Button";
import { cn } from "@/lib/cn";

/**
 * A control that navigates is an anchor, not a button. Wrapping a `<Link>` in a `<button>`
 * produces invalid markup and takes away middle-click, open-in-new-tab, and the link semantics
 * screen readers announce.
 */
export function LinkButton({
  variant = "secondary",
  size = "md",
  icon,
  className,
  children,
  ...props
}: ComponentProps<typeof Link> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
  icon?: ReactNode;
}) {
  return (
    <Link className={cn(buttonClasses(variant, size), className)} {...props}>
      {icon}
      {children}
    </Link>
  );
}
