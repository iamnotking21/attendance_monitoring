import { createWorkspace, createWorkspaceRequestSchema } from "@attendance/sync";
import type { NextRequest } from "next/server";

import {
  enforceRateLimit,
  ok,
  readJson,
  requireSyncConfigured,
  toErrorResponse,
} from "@/app/api/_lib/handler";

// postgres.js opens a TCP connection, which the edge runtime cannot do.
export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * Creates a workspace and enrols the calling device.
 *
 * The join code and token are returned exactly once. Neither is recoverable afterwards: the
 * server stores only their hashes, so a database leak yields no working credential.
 */
export async function POST(request: NextRequest) {
  const unavailable = requireSyncConfigured();
  if (unavailable) return unavailable;

  // Creating a workspace costs the server storage and costs the caller nothing, so it is the
  // endpoint most worth abusing.
  const limited = await enforceRateLimit(request, "workspace:create", 5, 3600);
  if (limited) return limited;

  const body = await readJson(request, createWorkspaceRequestSchema);
  if (!body.ok) return body.response;

  try {
    const created = await createWorkspace(body.data.name);
    return ok({
      workspaceId: created.workspace.id,
      name: created.workspace.name,
      joinCode: created.joinCode,
      token: created.token,
    });
  } catch (error) {
    return toErrorResponse(error);
  }
}
