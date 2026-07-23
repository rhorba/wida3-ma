import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  retries: 0,
  workers: 1,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:5176",
    video: "on",
    trace: "off",
    screenshot: "off",
  },
  outputDir: "../.recordings/raw",
  reporter: [["list"]],
});
