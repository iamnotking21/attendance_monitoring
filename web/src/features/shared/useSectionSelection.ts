"use client";

import { useLiveQuery } from "dexie-react-hooks";
import { useState } from "react";

import type { Section } from "@/domain/model";
import { listSections } from "@/lib/repositories/sections";

export interface SectionSelection {
  sections: Section[] | undefined;
  selected: Section | undefined;
  selectedId: number | undefined;
  select: (id: number) => void;
  loading: boolean;
}

/**
 * Keeps a section chosen at all times once any exist. Screens built around "the current
 * section" are useless with nothing selected, and asking the user to pick again every visit is
 * friction with no upside — so the first section stands in until they choose otherwise.
 */
export function useSectionSelection(): SectionSelection {
  const sections = useLiveQuery(() => listSections(), [], undefined);
  const [chosenId, setChosenId] = useState<number | undefined>();

  // Derived rather than synchronised through an effect: if the chosen section is deleted or has
  // not been picked yet, the first one stands in on this render instead of after an extra one.
  const selected = sections?.find((section) => section.id === chosenId) ?? sections?.[0];

  return {
    sections,
    selected,
    selectedId: selected?.id,
    select: setChosenId,
    loading: sections === undefined,
  };
}
