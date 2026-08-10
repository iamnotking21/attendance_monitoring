import type { Metadata, Viewport } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";

import { AppShell } from "@/components/layout/AppShell";
import { ToastProvider } from "@/components/ui/Toast";
import { AppBootstrap } from "@/features/app/AppBootstrap";

import "./globals.css";

// Self-hosted at build time by next/font, so there is no runtime request to a font CDN — one
// less third-party origin to allow through the content security policy, and no layout shift.
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

const mono = JetBrains_Mono({
  variable: "--font-mono-stack",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "Attendance Monitoring",
    template: "%s · Attendance Monitoring",
  },
  description:
    "QR-code attendance for schools. Scan a student ID and the record lands in the right schedule with the right status — present, late, or absent.",
  applicationName: "Attendance Monitoring",
  manifest: "/manifest.webmanifest",
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f6f7fb" },
    { media: "(prefers-color-scheme: dark)", color: "#0b0d14" },
  ],
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${inter.variable} ${mono.variable} h-full`}>
      <body className="min-h-dvh">
        <ToastProvider>
          <AppBootstrap />
          <AppShell>{children}</AppShell>
        </ToastProvider>
      </body>
    </html>
  );
}
