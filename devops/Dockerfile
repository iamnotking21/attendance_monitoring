# syntax=docker/dockerfile:1
#
# Build context is the repository root:
#   docker build -f devops/Dockerfile -t attendance-web .
#
# Three stages so the shipped image carries neither the source tree nor the build toolchain —
# only the standalone server Next.js emits, its trimmed dependency set, and the static assets.
#
# This is an npm workspace repo, so the install stage needs the root manifest and lockfile plus
# every workspace manifest. Copying only web/ would make `npm ci` reject the lockfile.
#
# The lockfile must be generated on Linux from the manifests alone, or it will not name the
# native bindings this image needs. npm records only the platforms present in the tree it is
# resolving, so a lockfile written on Windows contains no @tailwindcss/oxide-linux-*,
# lightningcss-linux-*, or @next/swc-linux-* entry, and the build dies with
# "Cannot find native binding". See docs/development.md for the one-line regeneration command.

# ---------------------------------------------------------------- dependencies
FROM node:22-alpine AS deps
WORKDIR /app

# Manifests only, so this layer is reused on every build that does not change dependencies.
COPY package.json package-lock.json ./
COPY web/package.json ./web/
COPY backend/package.json ./backend/

RUN npm ci --ignore-scripts

# ---------------------------------------------------------------------- build
FROM node:22-alpine AS builder
WORKDIR /app

ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
# Switches next.config.ts to `output: "standalone"`, which only the container needs.
ENV BUILD_STANDALONE=1

COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/web/node_modules ./web/node_modules
COPY package.json package-lock.json ./
COPY backend ./backend
COPY web ./web

RUN npm run build -w web

# --------------------------------------------------------------------- runner
FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# An unprivileged user: a process that never needs to write to the image should not be able to.
RUN addgroup --system --gid 1001 nodejs \
  && adduser --system --uid 1001 --ingroup nodejs nextjs

# In a workspace build the standalone output keeps the repository's directory shape, so the
# server entry point lives at web/server.js rather than at the root.
COPY --from=builder --chown=nextjs:nodejs /app/web/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/web/.next/static ./web/.next/static
COPY --from=builder --chown=nextjs:nodejs /app/web/public ./web/public

USER nextjs
EXPOSE 3000

# Probes the app itself rather than the port, so a server that is listening but broken is
# reported unhealthy instead of healthy.
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:3000/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["node", "web/server.js"]
