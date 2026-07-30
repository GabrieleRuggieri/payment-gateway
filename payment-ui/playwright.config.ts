import { defineConfig, devices } from '@playwright/test';

const useExisting = !!process.env.PLAYWRIGHT_NO_SERVER;
const baseURL = process.env.PLAYWRIGHT_BASE_URL
  ?? (useExisting ? 'http://127.0.0.1:3000' : 'http://127.0.0.1:4173');

/**
 * Smoke E2E.
 * - Default: avvia `vite preview` su :4173 (solo UI statica).
 * - Contro Docker: `PLAYWRIGHT_NO_SERVER=1 PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000 npm run test:e2e`
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
    ...devices['Desktop Chrome'],
  },
  webServer: useExisting
    ? undefined
    : {
        command: 'npm run preview -- --host 127.0.0.1 --port 4173',
        url: 'http://127.0.0.1:4173',
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
