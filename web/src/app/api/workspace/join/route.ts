import { joinWorkspace, joinWorkspaceRequestSchema } from "@attendance/sync";
import type { NextRequest } from "next/server";

import {
  enforceRateLimit,
  ok,
  readJson,
  requireSyncConfigured,
  toErrorResponse,
} from "@/app/api/_lib/handler";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/** Exchanges a join code for a token belonging to this device. */
export async function POST(request: NextRequest) {
  const unavailable = requireSyncConfigured();
  if (unavailable) return unavailable;

  // A join code is roughly 59 bits, so guessing it online is hopeless — but the limit is what
  // keeps it that way, by making an automated sweep cost real time.
  const limited = await enforceRateLimit(request, "workspace:join", 10, 900);
  if (limited) return limited;

  const body = await readJson(request, joinWorkspaceRequestSchema);
  if (!body.ok) return body.response;

  try {
    const joined = await joinWorkspace(body.data.joinCode);
    return ok({
      workspaceId: joined.workspace.id,
      name: joined.workspace.name,
      token: joined.token,
    });
  } catch (error) {
    return toErrorResponse(error);
  }
}
