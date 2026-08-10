"use client";

import {
  CalendarClock,
  Database,
  RefreshCw,
  LayoutDashboard,
  QrCode,
  ScrollText,
  Users,
  type LucideIcon,
} from "lucide-react";
import { m } from "motion/react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

import { cn } from "@/lib/cn";

interface NavItem {
  href: string;
  label: string;
  short: string;
  icon: LucideIcon;
}

const NAV: NavItem[] = [
  { href: "/", label: "Dashboard", short: "Today", icon: LayoutDashboard },
  { href: "/scan", label: "Scan", short: "Scan", icon: QrCode },
  { href: "/sections", label: "Sections", short: "Class", icon: Users },
  { href: "/schedules", label: "Schedules", short: "Sched", icon: CalendarClock },
  { href: "/reports", label: "Reports", short: "Report", icon: ScrollText },
  { href: "/data", label: "Data", short: "Data", icon: Database },
  { href: "/sync", label: "Sync", short: "Sync", icon: RefreshCw },
];

function isActive(pathname: string, href: string): boolean {
  return href === "/" ? pathname === "/" : pathname.startsWith(href);
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex min-h-dvh flex-col lg:flex-row">
      <SideNav pathname={pathname} />

      <main className="flex-1 pb-20 lg:pb-0">
        <div className="mx-auto w-full max-w-6xl px-4 py-6 sm:px-6 sm:py-8 2xl:max-w-[88rem]">{children}</div>
      </main>

      <BottomNav pathname={pathname} />
    </div>
  );
}

function SideNav({ pathname }: { pathname: string }) {
  return (
    <nav
      aria-label="Main"
      className="sticky top-0 hidden h-dvh w-60 shrink-0 flex-col border-r border-border bg-surface px-3 py-5 lg:flex"
    >
      <Link href="/" className="mb-6 flex items-center gap-2.5 px-2">
        <span className="grid size-9 place-items-center rounded-xl bg-primary text-on-primary">
          <QrCode aria-hidden className="size-4.5" />
        </span>
        <span className="min-w-0">
          <span className="block truncate text-sm font-semibold tracking-tight">
            Attendance
          </span>
          <span className="block truncate text-xs text-subtle">Monitoring</span>
        </span>
      </Link>

      <ul className="flex flex-col gap-0.5">
        {NAV.map((item) => {
          const active = isActive(pathname, item.href);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors duration-150",
                  active ? "text-primary" : "text-muted hover:bg-primary-soft hover:text-ink",
                )}
              >
                {active ? (
                  // One shared layout ID, so the highlight slides between items rather than
                  // blinking out in one place and in again in another.
                  <m.span
                    layoutId="nav-active"
                    className="absolute inset-0 rounded-lg bg-primary-soft"
                    transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                  />
                ) : null}
                <item.icon aria-hidden className="relative size-4.5" />
                <span className="relative">{item.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>

      <p className="mt-auto px-3 text-xs leading-relaxed text-subtle">
        Works offline. Data stays on this device unless you connect a workspace.
      </p>
    </nav>
  );
}

function BottomNav({ pathname }: { pathname: string }) {
  return (
    <nav
      aria-label="Main"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-surface/95 backdrop-blur lg:hidden"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      <ul className="scroll-x mx-auto flex max-w-lg">
        {NAV.map((item) => {
          const active = isActive(pathname, item.href);
          return (
            <li key={item.href} className="min-w-[3.25rem] flex-1">
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex min-h-14 flex-col items-center justify-center gap-1 px-1 py-2 text-[0.6875rem] font-medium transition-colors duration-150",
                  active ? "text-primary" : "text-muted",
                )}
              >
                <item.icon aria-hidden className="size-5" />
                {item.short}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
