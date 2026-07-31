import { test, expect, type Page } from "@playwright/test";
import { execSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PASSWORD = "Sprint2DemoPass!";

// Local dev DB runs in a container named wida3-dev-postgres, reached via `docker exec`.
// CI's Postgres is a plain service container on localhost, so it sets E2E_DB_EXEC to a
// TCP-based psql invocation instead (see .github/workflows/ci.yml, job "e2e").
const DB_EXEC = process.env.E2E_DB_EXEC ?? "docker exec wida3-dev-postgres psql -U wida3_app -d wida3";

function promoteToAdmin(email: string) {
  execSync(
    `${DB_EXEC} -c "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email='${email}' AND r.name='ADMIN';"`,
  );
}

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

  promoteToAdmin(adminEmail);

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

test("renter books an approved listing, sees the access code, then cancels the booking", async ({ page }) => {
  const stamp = Date.now();
  const city = `BookingDemo-${stamp}`;
  const ownerEmail = `owner-booking-${stamp}@wida3.test`;
  const adminEmail = `admin-booking-${stamp}@wida3.test`;
  const renterEmail = `renter-booking-${stamp}@wida3.test`;

  await registerOwnerAndCreateListing(page, ownerEmail, city, "Bookable warehouse");

  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Full name").fill("Booking Demo Admin");
  await page.getByLabel("Email").fill(adminEmail);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByRole("heading", { name: `Logged in as ${adminEmail}` })).toBeVisible();
  await page.getByRole("button", { name: "Log out" }).click();

  promoteToAdmin(adminEmail);

  await page.goto("/");
  await page.getByRole("button", { name: "Log in" }).click();
  await page.getByLabel("Email").fill(adminEmail);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Log in" }).click();
  await page.getByRole("listitem").filter({ hasText: "Bookable warehouse" }).getByRole("button", { name: "Approve" }).click();
  await page.getByRole("button", { name: "Log out" }).click();

  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Full name").fill("Booking Demo Renter");
  await page.getByLabel("Email").fill(renterEmail);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByRole("heading", { name: `Logged in as ${renterEmail}` })).toBeVisible();

  const searchSection = page.getByRole("heading", { name: "Search warehouses" }).locator("xpath=..");
  const bookingsSection = page.getByRole("heading", { name: "My bookings" }).locator("xpath=..");

  await searchSection.getByLabel("Location").fill(city);
  await searchSection.getByRole("button", { name: "Search" }).click();
  const resultItem = searchSection.getByRole("listitem").filter({ hasText: "Bookable warehouse" });
  await expect(resultItem).toBeVisible();
  await resultItem.getByRole("button", { name: "Book" }).click();

  const start = new Date();
  start.setDate(start.getDate() + 14);
  const end = new Date(start);
  end.setDate(end.getDate() + 7);
  const iso = (d: Date) => d.toISOString().slice(0, 10);
  await resultItem.getByLabel("Start date").fill(iso(start));
  await resultItem.getByLabel("End date").fill(iso(end));
  await resultItem.getByRole("button", { name: "Confirm booking" }).click();
  await expect(resultItem.getByText(/Booked! Access code:/)).toBeVisible({ timeout: 10_000 });

  const bookingItem = bookingsSection.getByRole("listitem").filter({ hasText: "Bookable warehouse" });
  await expect(bookingItem).toBeVisible();
  await expect(bookingItem.getByText("CONFIRMED")).toBeVisible();
  await expect(bookingItem.getByText(/access code:/)).toBeVisible();

  await bookingItem.getByRole("button", { name: "Cancel" }).click();
  await expect(bookingItem.getByText("CANCELLED")).toBeVisible({ timeout: 10_000 });
});

test("owner sees their listing in My Listings, edits it, then deactivates it", async ({ page }) => {
  const stamp = Date.now();
  const email = `owner-manage-${stamp}@wida3.test`;

  await page.goto("/");
  await page.getByRole("button", { name: "Register" }).click();
  await page.getByLabel("Full name").fill("Manage Demo Owner");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByLabel("I want to list a warehouse (Owner)").check();
  await page.getByRole("button", { name: "Register" }).click();
  await expect(page.getByRole("heading", { name: `Logged in as ${email}` })).toBeVisible();

  await page.getByLabel("Title").fill("Editable warehouse");
  await page.getByLabel("City").fill("Fes");
  await page.getByLabel("Address").fill("5 Rue Test");
  await page.getByLabel("Warehouse type").selectOption("DRY");
  await page.getByLabel(/Size \(sqm\)/).fill("120");
  await page.getByLabel("Weekly price").fill("400");
  await page.getByRole("button", { name: "Create listing" }).click();
  await expect(page.getByText("Listing created — pending admin approval.")).toBeVisible({ timeout: 10_000 });

  const myListingsSection = page.getByRole("heading", { name: "My listings" }).locator("xpath=..");
  const listingItem = myListingsSection.getByRole("listitem").filter({ hasText: "Editable warehouse" });
  await expect(listingItem).toBeVisible();
  await expect(listingItem.getByText("PENDING_APPROVAL")).toBeVisible();

  // Once editing starts, the listing's title moves from rendered text into an input's value, so
  // the hasText-based listingItem locator above would stop matching -- scope directly to the
  // section instead (there's exactly one listing here, so this stays unambiguous).
  await listingItem.getByRole("button", { name: "Edit" }).click();
  await myListingsSection.getByLabel("Edit title").fill("Edited warehouse title");
  await myListingsSection.getByRole("button", { name: "Save" }).click();

  const editedItem = myListingsSection.getByRole("listitem").filter({ hasText: "Edited warehouse title" });
  await expect(editedItem).toBeVisible();

  await editedItem.getByRole("button", { name: "Deactivate" }).click();
  await expect(editedItem.getByText("INACTIVE")).toBeVisible({ timeout: 10_000 });
  await expect(editedItem.getByRole("button", { name: "Edit" })).toHaveCount(0);
  await expect(editedItem.getByRole("button", { name: "Deactivate" })).toHaveCount(0);
});
