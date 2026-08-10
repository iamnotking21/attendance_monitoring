import type { Metadata } from "next";

import { SectionsView } from "@/features/sections/SectionsView";

export const metadata: Metadata = { title: "Sections" };

export default function SectionsPage() {
  return <SectionsView />;
}
