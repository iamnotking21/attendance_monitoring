import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";

import { AppShell } from "@/components/layout/AppShell";
import { MotionPreferences } from "@/components/motion/MotionPreferences";
import { ToastProvider } from "@/components/ui/Toast";
import { AppBootstrap } from "@/features/app/AppBootstrap";
import { ServiceWorker } from "@/features/app/ServiceWorker";

import "./globals.css";

// Self-hosted at build time by next/font, so there is no runtime request to a font CDN — one
// less third-party origin to allow through the content security policy, and no layout shift.
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

// Student numbers are set in a monospace face, but shipping a second webfont for a handful of
// short strings cost 40 kB compressed. The platform's own monospace font does the same job for
// nothing, and every target OS has a good one.

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
    <html lang="en" className={`${inter.variable} h-full`}>
      <body className="min-h-dvh">
        <MotionPreferences>
          <ToastProvider>
            <AppBootstrap />
            <ServiceWorker />
            <AppShell>{children}</AppShell>
          </ToastProvider>
        </MotionPreferences>
      </body>
    </html>
  );
}
