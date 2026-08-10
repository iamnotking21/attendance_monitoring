import { pull, pullRequestSchema } from "@attendance/sync";
import type { NextRequest } from "next/server";

import {
  ok,
  readJson,
  requireSyncConfigured,
  requireWorkspace,
  toErrorResponse,
} from "@/app/api/_lib/handler";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/** Returns everything in the workspace that changed after the caller's cursor. */
export async function POST(request: NextRequest) {
  const unavailable = requireSyncConfigured();
  if (unavailable) return unavailable;

  const auth = await requireWorkspace(request);
  if (!auth.ok) return auth.response;

  const body = await readJson(request, pullRequestSchema);
  if (!body.ok) return body.response;

  try {
    // The workspace comes from the token, never from the request body. Taking it from the body
    // would let any authenticated device read any other school's roster.
    return ok(await pull(auth.workspace.id, body.data.since, body.data.limit));
  } catch (error) {
    return toErrorResponse(error);
  }
}
