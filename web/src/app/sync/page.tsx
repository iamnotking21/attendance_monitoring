import type { Metadata } from "next";

import { SyncView } from "@/features/sync/SyncView";

export const metadata: Metadata = { title: "Sync" };

export default function SyncPage() {
  return <SyncView />;
}
