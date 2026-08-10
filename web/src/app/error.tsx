"use client";

import { TriangleAlert } from "lucide-react";
import { useEffect } from "react";

import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";

export default function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <Card>
      <EmptyState
        icon={TriangleAlert}
        title="Something went wrong"
        // The raw message is deliberately not shown: it is written for developers, and on this
        // screen it would only alarm a teacher who cannot act on it.
        description="This screen failed to load. Your stored attendance data has not been touched."
        action={<Button variant="primary" onClick={reset}>Try again</Button>}
      />
    </Card>
  );
}
