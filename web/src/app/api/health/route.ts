import { NextResponse } from "next/server";

/**
 * Liveness probe for the container. Deliberately says nothing about the application beyond
 * "the server is answering" — a health endpoint that leaks version numbers or dependency state
 * is free reconnaissance for anyone scanning the host.
 */
export const dynamic = "force-dynamic";

export function GET() {
  return NextResponse.json({ status: "ok" }, { headers: { "Cache-Control": "no-store" } });
}
