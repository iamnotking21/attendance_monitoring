import { notFound } from "next/navigation";

import { idSchema } from "@/domain/primitives";
import { StudentsView } from "@/features/students/StudentsView";

export default async function SectionStudentsPage(
  props: PageProps<"/sections/[sectionId]">,
) {
  const { sectionId } = await props.params;

  // The path segment is user input like any other. Anything that is not a UUID is a 404, not a
  // storage lookup with an arbitrary string in it.
  const parsed = idSchema.safeParse(sectionId);
  if (!parsed.success) notFound();

  return <StudentsView sectionId={parsed.data} />;
}
