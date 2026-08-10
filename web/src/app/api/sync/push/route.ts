import { push, pushRequestSchema } from "@attendance/sync";
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

/**
 * Accepts a batch of local changes.
 *
 * Safe to retry: rows are merged by last-write-wins and attendance records are deduplicated by
 * (student, schedule, date), so a client that never saw the response can send the same batch
 * again without creating anything twice.
 */
export async function POST(request: NextRequest) {
  const unavailable = requireSyncConfigured();
  if (unavailable) return unavailable;

  const auth = await requireWorkspace(request);
  if (!auth.ok) return auth.response;

  const body = await readJson(request, pushRequestSchema);
  if (!body.ok) return body.response;

  try {
    return ok(await push(auth.workspace.id, body.data.changes));
  } catch (error) {
    return toErrorResponse(error);
  }
}
