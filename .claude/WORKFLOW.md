# Execution workflow

Six specialist agents in `.claude/agents/`, two skills in `.claude/skills/`. This file defines
how they hand work to each other so nothing is audited before it exists and nothing ships
before it is audited.

## Agents

| Agent | Owns | Writes code |
|---|---|---|
| `architecture-refactorer` | layer boundaries, folder structure, naming, type safety | yes |
| `security-auditor` | secrets, input validation, injection, headers, CORS, storage | no — reports only |
| `performance-optimizer` | bundle size, code splitting, lazy loading, caching | yes |
| `ui-ux-engineer` | motion, responsive layout, accessibility | yes |
| `qa-docker-engineer` | unit/e2e tests, runtime crash hunt, Docker image | yes |
| `deploy-engineer` | Vercel config, deploy, live verification | yes |

## Skills

- `attendance-domain` — load **before** touching attendance, schedule, scan, or report logic.
  It is the authoritative statement of the ported business rules.
- `ship-pipeline` — the ordered quality gate. Load before any commit, image build, or deploy.

## Pipeline

```
  attendance-domain (read)
            │
            ▼
  architecture-refactorer ──▶ qa-docker-engineer (tests green)
            │                          │
            ├──────────┬───────────────┤
            ▼          ▼               ▼
    security-    performance-    ui-ux-engineer
     auditor      optimizer
            │          │               │
            └──────────┴───────────────┘
                       ▼
                 ship-pipeline (gates 0-5)
                       ▼
                 deploy-engineer (gate 6)
```

## Synchronization rules

1. **Structure before scrutiny.** `architecture-refactorer` settles the layout first. Auditing a
   tree that is about to move wastes the audit.
2. **Tests before parallel work.** `qa-docker-engineer` lands the domain test suite next. It is
   the safety net every later refactor leans on; without it, "behavior-preserving" is a claim
   rather than a fact.
3. **Fan out only after the net exists.** Security, performance, and UI/UX then run
   independently — they touch disjoint concerns and do not block each other.
4. **Security findings outrank performance wins.** When a fix and an optimization conflict, the
   fix wins; re-measure afterward.
5. **Any fix rewinds the pipeline.** A change at any stage re-runs every earlier gate. Evidence
   from before the change is void.
6. **Deploy last, and only on explicit user confirmation.** Production deployment is
   outward-facing and is never inferred from an earlier approval.

## Evidence standard

Every agent reports real command output, a real screenshot, or a `file:line` citation. No agent
reports a gate green that it did not observe green.
