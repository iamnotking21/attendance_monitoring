import { notFound } from "next/navigation";

import { StudentsView } from "@/features/students/StudentsView";

export default async function SectionStudentsPage(
  props: PageProps<"/sections/[sectionId]">,
) {
  const { sectionId } = await props.params;

  // The path segment is user input like any other. A non-numeric id is a 404, not a database
  // query with NaN in it.
  const id = Number(sectionId);
  if (!Number.isInteger(id) || id <= 0) notFound();

  return <StudentsView sectionId={id} />;
}
