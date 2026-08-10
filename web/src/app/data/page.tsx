import type { Metadata } from "next";

import { DataView } from "@/features/data/DataView";

export const metadata: Metadata = { title: "Data" };

export default function DataPage() {
  return <DataView />;
}
