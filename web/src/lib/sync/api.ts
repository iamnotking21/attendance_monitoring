import type {
  ChangeSet,
  PullResponse,
  PushResponse,
} from "@attendance/sync/protocol";

/**
 * Thin HTTP client for the sync endpoints.
 *
 * Every call is same-origin, which is what `connect-src 'self'` in the content security policy
 * permits and nothing more. Errors are classified rather than thrown as raw failures, because
 * the difference between "no network" and "this token was revoked" changes what the app should
 * do next.
 */

export type SyncFailure =
  /** The device is offline, or the request never reached the server. */
  | { kind: "offline"; message: string }
  /** The deployment has no database configured; the app is local-only here. */
  | { kind: "unavailable"; message: string }
  /** The token is no longer valid, so this device must reconnect. */
  | { kind: "unauthorized"; message: string }
  | { kind: "rejected"; message: string };

export class SyncError extends Error {
  readonly failure: SyncFailure;

  constructor(failure: SyncFailure) {
    super(failure.message);
    this.name = "SyncError";
    this.failure = failure;
  }
}

const REQUEST_TIMEOUT_MS = 20_000;

async function request<T>(path: string, body: unknown, token?: string): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response: Response;
  try {
    response = await fetch(path, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        ...(token ? { authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(body),
      signal: controller.signal,
      // Sync results must never come from a cache; a stale cursor would silently skip changes.
      cache: "no-store",
    });
  } catch {
    throw new SyncError({
      kind: "offline",
      message: "Could not reach the server. Your work is saved on this device.",
    });
  } finally {
    clearTimeout(timeout);
  }

  if (response.ok) return (await response.json()) as T;

  const problem = await response
    .json()
    .catch(() => ({ error: "The server returned an unexpected response.", code: "rejected" }));

  const message = typeof problem.error === "string" ? problem.error : "Sync failed.";

  if (response.status === 401) throw new SyncError({ kind: "unauthorized", message });
  if (response.status === 503) throw new SyncError({ kind: "unavailable", message });
  throw new SyncError({ kind: "rejected", message });
}

export interface WorkspaceResponse {
  workspaceId: string;
  name: string;
  joinCode?: string;
  token: string;
}

export async function isSyncAvailable(): Promise<boolean> {
  try {
    const response = await fetch("/api/sync/status", { cache: "no-store" });
    if (!response.ok) return false;
    const body = (await response.json()) as { configured?: boolean };
    return body.configured === true;
  } catch {
    return false;
  }
}

export function createWorkspace(name: string): Promise<WorkspaceResponse> {
  return request<WorkspaceResponse>("/api/workspace", { name });
}

export function joinWorkspace(joinCode: string): Promise<WorkspaceResponse> {
  return request<WorkspaceResponse>("/api/workspace/join", { joinCode });
}

export function pullChanges(
  token: string,
  since: number,
  limit = 500,
): Promise<PullResponse> {
  return request<PullResponse>("/api/sync/pull", { since, limit }, token);
}

export function pushChanges(token: string, changes: ChangeSet): Promise<PushResponse> {
  return request<PushResponse>("/api/sync/push", { changes }, token);
}
