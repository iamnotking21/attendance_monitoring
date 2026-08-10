"use client";

import { useLiveQuery } from "dexie-react-hooks";
import {
  Cloud,
  CloudOff,
  Copy,
  LogIn,
  RefreshCw,
  Unplug,
  Wifi,
  WifiOff,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Field, TextInput } from "@/components/ui/Field";
import { errorMessage, useToast } from "@/components/ui/Toast";
import { now } from "@/domain/primitives";
import {
  createWorkspace as createRemoteWorkspace,
  isSyncAvailable,
  joinWorkspace as joinRemoteWorkspace,
} from "@/lib/sync/api";
import { isOnline, syncNow } from "@/lib/sync/engine";
import {
  clearConnection,
  readConnection,
  readLastSyncedAt,
  writeConnection,
} from "@/lib/sync/state";
import { cn } from "@/lib/cn";

export function SyncView() {
  const toast = useToast();
  const [available, setAvailable] = useState<boolean | null>(null);
  const [online, setOnline] = useState(true);
  const [busy, setBusy] = useState(false);
  const [confirmDisconnect, setConfirmDisconnect] = useState(false);
  const [workspaceName, setWorkspaceName] = useState("");
  const [joinCode, setJoinCode] = useState("");
  const [error, setError] = useState<string>();

  const connection = useLiveQuery(() => readConnection(), [], undefined);
  const lastSyncedAt = useLiveQuery(() => readLastSyncedAt(), [], undefined);

  useEffect(() => {
    void isSyncAvailable().then(setAvailable);
  }, []);

  useEffect(() => {
    const update = () => setOnline(isOnline());
    update();
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    return () => {
      window.removeEventListener("online", update);
      window.removeEventListener("offline", update);
    };
  }, []);

  /**
   * A first sync that fails is not a detail to swallow. Connecting and then silently sending
   * nothing looks identical to working, and the operator only finds out when the second device
   * shows an empty roster.
   */
  const reportFirstSync = useCallback(
    (result: Awaited<ReturnType<typeof syncNow>>) => {
      if (result.ok) return;
      setError(result.failure.message);
      toast.error(`Connected, but the first sync failed: ${result.failure.message}`);
    },
    [toast],
  );

  const connect = useCallback(
    async (action: () => Promise<void>) => {
      setBusy(true);
      setError(undefined);
      try {
        await action();
      } catch (caught) {
        const message = errorMessage(caught, "Could not connect.");
        setError(message);
        toast.error(message);
      } finally {
        setBusy(false);
      }
    },
    [toast],
  );

  async function handleCreate() {
    await connect(async () => {
      const created = await createRemoteWorkspace(workspaceName);
      await writeConnection({
        workspaceId: created.workspaceId,
        workspaceName: created.name,
        token: created.token,
        joinCode: created.joinCode,
        connectedAt: now(),
      });
      reportFirstSync(await syncNow());
      toast.success(`Connected to ${created.name}.`);
      setWorkspaceName("");
    });
  }

  async function handleJoin() {
    await connect(async () => {
      const joined = await joinRemoteWorkspace(joinCode);
      await writeConnection({
        workspaceId: joined.workspaceId,
        workspaceName: joined.name,
        token: joined.token,
        connectedAt: now(),
      });
      reportFirstSync(await syncNow());
      toast.success(`Joined ${joined.name}.`);
      setJoinCode("");
    });
  }

  async function handleSyncNow() {
    setBusy(true);
    try {
      const result = await syncNow();
      if (result.ok) {
        toast.success(
          `Sent ${result.outcome.pushed}, received ${result.outcome.pulled}.`,
        );
      } else {
        toast.error(result.failure.message);
      }
    } finally {
      setBusy(false);
    }
  }

  async function handleDisconnect() {
    setConfirmDisconnect(false);
    await clearConnection();
    toast.info("Disconnected. Everything on this device was kept.");
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Sync"
        description="Attendance works entirely offline. Connecting a workspace also keeps several devices in step."
      />

      <Card>
        <CardBody className="flex flex-wrap items-center gap-x-6 gap-y-3">
          <StatusPill
            icon={online ? Wifi : WifiOff}
            label={online ? "Online" : "Offline"}
            tone={online ? "good" : "muted"}
          />
          <StatusPill
            icon={connection ? Cloud : CloudOff}
            label={connection ? `Workspace: ${connection.workspaceName}` : "This device only"}
            tone={connection ? "good" : "muted"}
          />
          {lastSyncedAt ? (
            <span className="text-sm text-muted">
              Last synced {new Date(lastSyncedAt).toLocaleString()}
            </span>
          ) : null}
        </CardBody>
      </Card>

      {available === false ? (
        <Card>
          <CardHeader
            title="Sync is not enabled on this deployment"
            description="No database is configured, so there is nothing to sync to. Everything below is unavailable — the app itself is unaffected and keeps working on this device."
          />
        </Card>
      ) : null}

      {connection ? (
        <>
          <Card>
            <CardHeader
              title="Connected"
              description="Changes made here are sent to the workspace, and changes from other devices arrive on the next sync."
            />
            <CardBody className="flex flex-wrap gap-2">
              <Button
                variant="primary"
                disabled={busy || !online}
                icon={<RefreshCw aria-hidden className={cn("size-4", busy && "animate-spin")} />}
                onClick={handleSyncNow}
              >
                {busy ? "Syncing…" : "Sync now"}
              </Button>
              <Button
                disabled={busy}
                icon={<Unplug aria-hidden className="size-4" />}
                onClick={() => setConfirmDisconnect(true)}
              >
                Disconnect
              </Button>
            </CardBody>
          </Card>

          {connection.joinCode ? (
            <Card>
              <CardHeader
                title="Join code"
                description="Enter this on another device to connect it to the same workspace. Anyone with the code gets full access, so share it the way you would a key."
              />
              <CardBody className="flex flex-wrap items-center gap-3">
                <code className="rounded-lg border border-border bg-canvas px-3 py-2 font-mono text-base tracking-widest">
                  {connection.joinCode}
                </code>
                <Button
                  icon={<Copy aria-hidden className="size-4" />}
                  onClick={async () => {
                    await navigator.clipboard.writeText(connection.joinCode ?? "");
                    toast.success("Join code copied.");
                  }}
                >
                  Copy
                </Button>
              </CardBody>
            </Card>
          ) : null}
        </>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader
              title="Create a workspace"
              description="Start here on the first device. You will get a join code for the others."
            />
            <CardBody>
              <form
                className="flex flex-col gap-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  void handleCreate();
                }}
              >
                <Field label="School or workspace name" error={error}>
                  {({ id, describedBy, invalid }) => (
                    <TextInput
                      id={id}
                      value={workspaceName}
                      maxLength={80}
                      placeholder="Pedro Fernandez National High School"
                      aria-describedby={describedBy}
                      aria-invalid={invalid}
                      disabled={available === false}
                      onChange={(event) => setWorkspaceName(event.target.value)}
                    />
                  )}
                </Field>
                <Button
                  type="submit"
                  variant="primary"
                  className="self-start"
                  disabled={busy || available === false || workspaceName.trim() === ""}
                >
                  Create workspace
                </Button>
              </form>
            </CardBody>
          </Card>

          <Card>
            <CardHeader
              title="Join an existing workspace"
              description="Enter the code shown on the device that created it."
            />
            <CardBody>
              <form
                className="flex flex-col gap-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  void handleJoin();
                }}
              >
                <Field label="Join code">
                  {({ id }) => (
                    <TextInput
                      id={id}
                      value={joinCode}
                      maxLength={32}
                      placeholder="ABCD-EFGH-JKLM"
                      className="font-mono tracking-widest uppercase"
                      autoComplete="off"
                      disabled={available === false}
                      onChange={(event) => setJoinCode(event.target.value)}
                    />
                  )}
                </Field>
                <Button
                  type="submit"
                  variant="primary"
                  className="self-start"
                  icon={<LogIn aria-hidden className="size-4" />}
                  disabled={busy || available === false || joinCode.trim().length < 8}
                >
                  Join workspace
                </Button>
              </form>
            </CardBody>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader title="How it behaves offline" />
        <CardBody className="flex flex-col gap-2 text-sm text-muted">
          <p>
            Scanning, editing, and reporting all read and write this device&apos;s own storage, so
            they work with no network at all. Sync runs in the background and simply has nothing
            to do until a connection returns.
          </p>
          <p>
            When two devices change the same section, student, or schedule while both offline, the
            most recent edit wins once they reconnect. Attendance records never conflict: they are
            only ever added, and one student can hold only one record per schedule per day.
          </p>
        </CardBody>
      </Card>

      <ConfirmDialog
        open={confirmDisconnect}
        title="Disconnect this device?"
        description="It stops sending and receiving changes. Every section, student, schedule, and attendance record already on this device is kept."
        confirmLabel="Disconnect"
        onConfirm={handleDisconnect}
        onCancel={() => setConfirmDisconnect(false)}
      />
    </div>
  );
}

function StatusPill({
  icon: Icon,
  label,
  tone,
}: {
  icon: typeof Wifi;
  label: string;
  tone: "good" | "muted";
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-2 text-sm font-medium",
        tone === "good" ? "text-present" : "text-muted",
      )}
    >
      <Icon aria-hidden className="size-4" />
      {label}
    </span>
  );
}
