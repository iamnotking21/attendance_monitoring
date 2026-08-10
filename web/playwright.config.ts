import { defineConfig, devices } from "@playwright/test";

const PORT = Number(process.env.E2E_PORT ?? 3100);
const baseURL = `http://127.0.0.1:${PORT}`;

/**
 * End-to-end tests run against a production build, not the dev server: development mode carries
 * a looser CSP and Strict Mode double-invocation, so a suite that only ever sees `next dev`
 * proves nothing about what ships.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? "line" : "list",
  timeout: 30_000,

  use: {
    baseURL,
    trace: "on-first-retry",
    // The scanner asks for a camera. Granting it up front keeps the permission prompt from
    // blocking, and Chromium's fake device makes the code path runnable without hardware.
    permissions: ["camera"],
  },

  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        launchOptions: {
          args: ["--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream"],
        },
      },
    },
    // Pixel 5 rather than an iPhone profile: it exercises a real phone viewport and touch
    // emulation on the Chromium that is already installed, instead of pulling down WebKit.
    { name: "mobile", use: { ...devices["Pixel 5"] } },
  ],

  webServer: {
    command: `npm run build && npm run start -- --port ${PORT}`,
    url: `${baseURL}/api/health`,
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
});
