import {
  authenticate,
  bearerToken,
  clientKey,
  consumeRateLimit,
  DatabaseNotConfiguredError,
  InvalidJoinCodeError,
  isSyncConfigured,
  UnauthorizedError,
  type Workspace,
} from "@attendance/sync";
import { NextResponse, type NextRequest } from "next/server";
import type { z } from "zod";

/**
 * Shared plumbing for the sync endpoints: JSON parsing, schema validation, authentication, rate
 * limiting, and error mapping.
 *
 * Every route goes through here so that none of them can forget one of those steps. Error
 * responses carry a stable machine-readable `code` and a message written for a person, and never
 * echo an internal exception — a stack trace or a driver error is free reconnaissance.
 */

export type ApiCode =
  | "unauthorized"
  | "not_found"
  | "invalid_request"
  | "rate_limited"
  | "unavailable";

const STATUS: Record<ApiCode, number> = {
  unauthorized: 401,
  not_found: 404,
  invalid_request: 400,
  rate_limited: 429,
  unavailable: 503,
};

export function fail(code: ApiCode, error: string, headers?: HeadersInit): NextResponse {
  return NextResponse.json(
    { error, code },
    { status: STATUS[code], headers: { "Cache-Control": "no-store", ...headers } },
  );
}

export function ok<T>(body: T): NextResponse {
  return NextResponse.json(body, { headers: { "Cache-Control": "no-store" } });
}

/** A sync payload has a hard ceiling; the schemas cap row counts, this caps raw bytes. */
const MAX_BODY_BYTES = 8 * 1024 * 1024;

export async function readJson<S extends z.ZodType>(
  request: NextRequest,
  schema: S,
): Promise<{ ok: true; data: z.infer<S> } | { ok: false; response: NextResponse }> {
  const declared = Number(request.headers.get("content-length") ?? 0);
  if (declared > MAX_BODY_BYTES) {
    return { ok: false, response: fail("invalid_request", "That request body is too large.") };
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return { ok: false, response: fail("invalid_request", "The request body is not valid JSON.") };
  }

  const parsed = schema.safeParse(raw);
  if (!parsed.success) {
    const issue = parsed.error.issues[0];
    const where = issue?.path.length ? ` at ${issue.path.join(".")}` : "";
    return {
      ok: false,
      response: fail(
        "invalid_request",
        `The request is not valid${where}: ${issue?.message ?? "unrecognised shape"}`,
      ),
    };
  }

  return { ok: true, data: parsed.data };
}

export function requireSyncConfigured(): NextResponse | null {
  if (isSyncConfigured()) return null;
  return fail(
    "unavailable",
    "Sync is not enabled on this deployment. The app still works offline on this device.",
  );
}

export async function requireWorkspace(
  request: NextRequest,
): Promise<{ ok: true; workspace: Workspace } | { ok: false; response: NextResponse }> {
  try {
    const workspace = await authenticate(bearerToken(request.headers.get("authorization")));
    return { ok: true, workspace };
  } catch (error) {
    return { ok: false, response: toErrorResponse(error) };
  }
}

/**
 * Rate limits by caller. Applied to workspace creation and joining, where repetition is what an
 * attacker gets value from: unlimited workspace creation is free storage, and unlimited joining
 * is an offline-free brute force against the join code.
 */
export async function enforceRateLimit(
  request: NextRequest,
  prefix: string,
  limit: number,
  windowSeconds: number,
): Promise<NextResponse | null> {
  const key = clientKey(prefix, request.headers.get("x-forwarded-for"));
  const result = await consumeRateLimit(key, limit, windowSeconds);

  if (result.allowed) return null;

  return fail("rate_limited", "Too many attempts. Wait a moment and try again.", {
    "Retry-After": String(result.retryAfterSeconds),
  });
}

export function toErrorResponse(error: unknown): NextResponse {
  if (error instanceof UnauthorizedError) {
    return fail("unauthorized", "This device is not connected to a workspace.");
  }
  if (error instanceof InvalidJoinCodeError) {
    return fail("not_found", "That join code does not match any workspace.");
  }
  if (error instanceof DatabaseNotConfiguredError) {
    return fail("unavailable", "Sync is not enabled on this deployment.");
  }

  // Anything else is a bug or an outage. It is logged for the operator and described generically
  // to the caller.
  console.error("Sync request failed:", error);
  return fail("unavailable", "Sync is temporarily unavailable. Your data is safe on this device.");
}
