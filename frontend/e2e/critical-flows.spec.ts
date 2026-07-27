import { test, expect, type Page } from "@playwright/test";
import { execSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PASSWORD = "Sprint2DemoPass!";

async function registerOwnerAndCreateListing(page: Page, email: string, city: string, title: string) {
  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Full name").fill("Search Demo Owner");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByLabel("I want to list a warehouse (Owner)").check();
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByRole("heading", { name: `Logged in as ${email}` })).toBeVisible();

  await page.getByLabel("Title").fill(title);
  await page.getByLabel("City").fill(city);
  await page.getByLabel("Address").fill("1 Test Street");
  await page.getByLabel("Warehouse type").selectOption("DRY");
  await page.getByLabel(/Size \(sqm\)/).fill("100");
  await page.getByLabel("Weekly price").fill("500");
  await page.getByRole("button", { name: "Create listing" }).click();
  await expect(page.getByText("Listing created — pending admin approval.")).toBeVisible({ timeout: 10_000 });

  await page.getByRole("button", { name: "Log out" }).click();
}

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

test("admin approves one listing and rejects another; only the approved one is publicly searchable", async ({
  page,
}) => {
  const stamp = Date.now();
  const adminEmail = `admin-${stamp}@wida3.test`;
  const approvedCity = `SearchDemoApproved-${stamp}`;
  const rejectedCity = `SearchDemoRejected-${stamp}`;

  await registerOwnerAndCreateListing(page, `owner-approve-${stamp}@wida3.test`, approvedCity, "Approve me warehouse");
  await registerOwnerAndCreateListing(page, `owner-reject-${stamp}@wida3.test`, rejectedCity, "Reject me warehouse");

  // ADMIN is not self-assignable at registration (mirrors production: admins are provisioned
  // out-of-band). Register a plain account, then promote it directly in the dev Postgres.
  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Full name").fill("Search Demo Admin");
  await page.getByLabel("Email").fill(adminEmail);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByRole("heading", { name: `Logged in as ${adminEmail}` })).toBeVisible();
  await page.getByRole("button", { name: "Log out" }).click();

  execSync(
    `docker exec wida3-dev-postgres psql -U wida3_app -d wida3 -c "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email='${adminEmail}' AND r.name='ADMIN';"`,
  );

  await page.goto("/");
  await page.getByRole("button", { name: "Log in" }).click();
  await page.getByLabel("Email").fill(adminEmail);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page.getByRole("heading", { name: `Logged in as ${adminEmail}` })).toBeVisible();

  const approveItem = page.getByRole("listitem").filter({ hasText: "Approve me warehouse" });
  await approveItem.getByRole("button", { name: "Approve" }).click();
  await expect(approveItem).toHaveCount(0);

  const rejectItem = page.getByRole("listitem").filter({ hasText: "Reject me warehouse" });
  await rejectItem.getByPlaceholder("Reason for rejection").fill("Not a real warehouse");
  await rejectItem.getByRole("button", { name: "Reject" }).click();
  await expect(rejectItem).toHaveCount(0);

  await page.getByRole("button", { name: "Log out" }).click();

  await page.goto("/");
  await page.getByLabel("Location").fill(approvedCity);
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByText("Approve me warehouse")).toBeVisible();

  await page.getByLabel("Location").fill(rejectedCity);
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByText("No warehouses match")).toBeVisible();
});
