import { isSyncConfigured, PROTOCOL_VERSION } from "@attendance/sync";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * Whether this deployment has a database behind it.
 *
 * The client asks before showing any sync UI, so a deployment without `DATABASE_URL` presents an
 * honest "offline only" state instead of buttons that fail when pressed. Deliberately says
 * nothing further — no host, no version, no connection detail.
 */
export function GET() {
  return NextResponse.json(
    { configured: isSyncConfigured(), protocol: PROTOCOL_VERSION },
    { headers: { "Cache-Control": "no-store" } },
  );
}
