import { Compass } from "lucide-react";

import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { LinkButton } from "@/components/ui/LinkButton";

export default function NotFound() {
  return (
    <Card>
      <EmptyState
        icon={Compass}
        title="Page not found"
        description="That address does not match anything in this app."
        action={
          <LinkButton href="/" variant="primary">
            Back to today
          </LinkButton>
        }
      />
    </Card>
  );
}
