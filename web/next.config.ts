import type { NextConfig } from "next";

/**
 * Content Security Policy.
 *
 * `default-src 'none'` means every directive below is an explicit allowance rather than an
 * exception, so a resource type nobody thought about is blocked instead of permitted.
 *
 * `script-src` carries `'unsafe-inline'`, which is a real weakening and is called out in
 * `docs/security-audit.md`. Next.js injects inline bootstrap and streaming scripts, and the
 * alternative — a per-request nonce from a proxy — forces every page out of static rendering.
 * For an application that has no server, no session, no cookie, and no cross-origin request,
 * that trade buys very little; the injection surface that matters here is the DOM, which is
 * closed off by rendering all stored values as text rather than as markup.
 */
const CSP = [
  "default-src 'none'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "font-src 'self'",
  // data: for generated QR codes, blob: for downloads the app builds in the browser.
  "img-src 'self' data: blob:",
  // The QR decoder runs in a worker created from a blob URL.
  "worker-src 'self' blob:",
  "connect-src 'self'",
  "manifest-src 'self'",
  "media-src 'self' blob:",
  "form-action 'self'",
  "base-uri 'none'",
  "object-src 'none'",
  "frame-ancestors 'none'",
  "upgrade-insecure-requests",
].join("; ");

const SECURITY_HEADERS = [
  { key: "Content-Security-Policy", value: CSP },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Cross-Origin-Opener-Policy", value: "same-origin" },
  { key: "Cross-Origin-Resource-Policy", value: "same-origin" },
  {
    key: "Strict-Transport-Security",
    value: "max-age=63072000; includeSubDomains; preload",
  },
  {
    // The camera is the one capability this app needs. Everything else is refused outright, so
    // a compromised dependency cannot quietly reach for location or a microphone.
    key: "Permissions-Policy",
    value: [
      "camera=(self)",
      "microphone=()",
      "geolocation=()",
      "payment=()",
      "usb=()",
      "interest-cohort=()",
    ].join(", "),
  },
];

const nextConfig: NextConfig = {
  // Emits a self-contained server bundle, which keeps the Docker runtime layer small.
  output: "standalone",
  poweredByHeader: false,
  reactStrictMode: true,

  // Import only the icons actually referenced, instead of the whole lucide barrel.
  experimental: {
    optimizePackageImports: ["lucide-react"],
  },

  async headers() {
    return [
      { source: "/:path*", headers: SECURITY_HEADERS },
      // `/_next/static` is deliberately left alone: Next.js already serves it content-hashed
      // and immutable, and overriding it breaks the dev server's own cache behaviour.
      {
        source: "/icon.svg",
        headers: [
          { key: "Cache-Control", value: "public, max-age=86400, stale-while-revalidate=604800" },
        ],
      },
    ];
  },
};

export default nextConfig;
