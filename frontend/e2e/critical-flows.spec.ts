import { test, expect } from "@playwright/test";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

test("owner registers, creates a listing, logs out, and logs back in", async ({ page }) => {
  const stamp = Date.now();
  const email = `owner-${stamp}@wida3.test`;
  const password = "Sprint2DemoPass!";

  await page.goto("/");

  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Full name").fill("Amine Owner");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByLabel("I want to list a warehouse (Owner)").check();
  await page.getByRole("button", { name: "Register" }).click();

  await expect(page.getByRole("heading", { name: `Logged in as ${email}` })).toBeVisible();
  await expect(page.getByText("Roles: OWNER")).toBeVisible();

  await page.getByLabel("Title").fill("Cold storage warehouse — Casablanca port zone");
  await page.getByLabel("City").fill("Casablanca");
  await page.getByLabel("Address").fill("12 Rue du Port, Casablanca");
  await page.getByLabel("Warehouse type").selectOption("COLD");
  await page.getByLabel(/Size \(sqm\)/).fill("450");
  await page.getByLabel("Weekly price").fill("3200");
  await page.getByLabel(/Photos \(up to/).setInputFiles(path.join(__dirname, "fixtures/test-photo.png"));

  await page.getByRole("button", { name: "Create listing" }).click();
  await expect(page.getByText("Listing created — pending admin approval.")).toBeVisible({ timeout: 10_000 });

  await page.getByRole("button", { name: "Log out" }).click();
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page.getByRole("heading", { name: "Log in" })).toBeVisible();

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page.getByRole("heading", { name: `Logged in as ${email}` })).toBeVisible();
});
