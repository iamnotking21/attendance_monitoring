export {
  authenticate,
  bearerToken,
  createWorkspace,
  generateJoinCode,
  generateToken,
  hashSecret,
  InvalidJoinCodeError,
  joinWorkspace,
  revokeDevice,
  UnauthorizedError,
  type EnrolledDevice,
  type Workspace,
} from "./auth";

export {
  closeDatabase,
  db,
  DatabaseNotConfiguredError,
  isSyncConfigured,
  __setDatabaseForTests,
  type Database,
} from "./client";

export { clientKey, consumeRateLimit, type RateLimitResult } from "./rateLimit";

export { currentCursor, pull, push } from "./sync";

export * from "./protocol";
