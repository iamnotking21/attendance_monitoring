-- Attendance Monitoring sync server, initial schema.
--
-- One sequence shared by every replicated table. Each table having its own would make a single
-- pull cursor impossible: "everything after 40" would mean a different point in each table, and
-- devices would skip rows or replay them forever.
CREATE SEQUENCE IF NOT EXISTS sync_seq;

CREATE TABLE IF NOT EXISTS workspaces (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name            text NOT NULL,
  join_code_hash  text NOT NULL UNIQUE,
  created_at      timestamptz NOT NULL DEFAULT now()
);

-- One row per device. Per-device tokens mean a lost phone can be revoked on its own.
CREATE TABLE IF NOT EXISTS device_tokens (
  token_hash    text PRIMARY KEY,
  workspace_id  uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  created_at    timestamptz NOT NULL DEFAULT now(),
  last_seen_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS device_tokens_workspace ON device_tokens (workspace_id);

CREATE TABLE IF NOT EXISTS sections (
  workspace_id  uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  id            uuid NOT NULL,
  name          text NOT NULL,
  archived      boolean NOT NULL DEFAULT false,
  created_at    timestamptz NOT NULL,
  updated_at    timestamptz NOT NULL,
  server_seq    bigint NOT NULL DEFAULT nextval('sync_seq')
);
CREATE UNIQUE INDEX IF NOT EXISTS sections_pk ON sections (workspace_id, id);
CREATE INDEX IF NOT EXISTS sections_cursor ON sections (workspace_id, server_seq);

CREATE TABLE IF NOT EXISTS students (
  workspace_id    uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  id              uuid NOT NULL,
  section_id      uuid NOT NULL,
  student_number  text NOT NULL,
  last_name       text NOT NULL,
  first_name      text NOT NULL,
  middle_name     text NOT NULL DEFAULT '',
  gender          text NOT NULL,
  archived        boolean NOT NULL DEFAULT false,
  created_at      timestamptz NOT NULL,
  updated_at      timestamptz NOT NULL,
  server_seq      bigint NOT NULL DEFAULT nextval('sync_seq')
);
CREATE UNIQUE INDEX IF NOT EXISTS students_pk ON students (workspace_id, id);
CREATE INDEX IF NOT EXISTS students_cursor ON students (workspace_id, server_seq);
CREATE INDEX IF NOT EXISTS students_number ON students (workspace_id, student_number);

CREATE TABLE IF NOT EXISTS schedules (
  workspace_id  uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  id            uuid NOT NULL,
  section_id    uuid NOT NULL,
  title         text NOT NULL,
  venue         text NOT NULL DEFAULT '',
  present       jsonb NOT NULL,
  late          jsonb NOT NULL,
  archived      boolean NOT NULL DEFAULT false,
  created_at    timestamptz NOT NULL,
  updated_at    timestamptz NOT NULL,
  server_seq    bigint NOT NULL DEFAULT nextval('sync_seq')
);
CREATE UNIQUE INDEX IF NOT EXISTS schedules_pk ON schedules (workspace_id, id);
CREATE INDEX IF NOT EXISTS schedules_cursor ON schedules (workspace_id, server_seq);

CREATE TABLE IF NOT EXISTS attendance_records (
  workspace_id    uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  id              uuid NOT NULL,
  schedule_id     uuid NOT NULL,
  section_id      uuid NOT NULL,
  student_number  text NOT NULL,
  date            date NOT NULL,
  status          text NOT NULL,
  schedule_title  text NOT NULL DEFAULT '',
  recorded_at     timestamptz NOT NULL,
  server_seq      bigint NOT NULL DEFAULT nextval('sync_seq')
);
CREATE UNIQUE INDEX IF NOT EXISTS records_pk ON attendance_records (workspace_id, id);
-- One record per student, per schedule, per day — the same invariant the device enforces,
-- restated where two devices meet.
CREATE UNIQUE INDEX IF NOT EXISTS records_natural_key
  ON attendance_records (workspace_id, student_number, schedule_id, date);
CREATE INDEX IF NOT EXISTS records_cursor ON attendance_records (workspace_id, server_seq);

CREATE TABLE IF NOT EXISTS school_days (
  workspace_id   uuid NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
  date           date NOT NULL,
  first_seen_at  timestamptz NOT NULL,
  server_seq     bigint NOT NULL DEFAULT nextval('sync_seq')
);
CREATE UNIQUE INDEX IF NOT EXISTS school_days_pk ON school_days (workspace_id, date);
CREATE INDEX IF NOT EXISTS school_days_cursor ON school_days (workspace_id, server_seq);

CREATE TABLE IF NOT EXISTS rate_limits (
  key           text PRIMARY KEY,
  window_start  timestamptz NOT NULL,
  count         integer NOT NULL DEFAULT 0
);
