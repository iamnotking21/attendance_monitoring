import type { Metadata } from "next";

import { ScannerView } from "@/features/scanner/ScannerView";

export const metadata: Metadata = { title: "Scan" };

export default function ScannerPage() {
  return <ScannerView />;
}
