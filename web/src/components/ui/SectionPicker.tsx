"use client";

import type { Section } from "@/domain/model";
import { SelectInput } from "@/components/ui/Field";

export function SectionPicker({
  sections,
  value,
  onChange,
  label = "Section",
}: {
  sections: Section[];
  value: number | undefined;
  onChange: (id: number) => void;
  label?: string;
}) {
  return (
    <label className="flex items-center gap-2 text-sm">
      <span className="sr-only sm:not-sr-only sm:text-muted">{label}</span>
      <SelectInput
        value={value ?? ""}
        onChange={(event) => onChange(Number(event.target.value))}
        className="w-auto min-w-44"
        aria-label={label}
      >
        {sections.map((section) => (
          <option key={section.id} value={section.id}>
            {section.name}
          </option>
        ))}
      </SelectInput>
    </label>
  );
}
