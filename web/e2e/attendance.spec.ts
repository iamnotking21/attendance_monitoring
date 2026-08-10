import { expect, test, type Page } from "@playwright/test";

/**
 * Journeys a coordinator actually performs. Each test starts from a clean IndexedDB so the
 * demo seed runs fresh and one test can never depend on another having gone first.
 */

async function clearStorage(page: Page) {
  await page.goto("/");
  await page.evaluate(async () => {
    const databases = await indexedDB.databases();
    await Promise.all(
      databases
        .filter((entry) => entry.name === "attendance_monitoring")
        .map(
          (entry) =>
            new Promise<void>((resolve) => {
              const request = indexedDB.deleteDatabase(entry.name!);
              request.onsuccess = request.onerror = request.onblocked = () => resolve();
            }),
        ),
    );
  });
  await page.reload();
}

test.beforeEach(async ({ page }) => {
  const errors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });
  page.on("pageerror", (error) => errors.push(error.message));

  await clearStorage(page);

  // Any console error at all fails the test. A page that "works" while shouting into the
  // console is a page with a bug nobody has hit yet.
  test.info().annotations.push({ type: "console-errors", description: String(errors.length) });
  expect(errors, errors.join("\n")).toEqual([]);
});

test("dashboard shows the seeded roster", async ({ page }) => {
  await expect(page.getByRole("heading", { name: "Today" })).toBeVisible();
  await expect(page.getByText("Grade 11 - Rizal").first()).toBeVisible();
  await expect(page.getByRole("tab", { name: /Present/ })).toBeVisible();
});

test("a typed student number is recorded and reaches the dashboard", async ({ page }) => {
  await page.goto("/scan");

  await page.getByLabel("Student number").fill("2024-1001");
  await page.getByRole("button", { name: "Record" }).click();

  const feed = page.getByText("Dela Cruz, Juan R.").first();
  await expect(feed).toBeVisible();
  await expect(page.getByText("Homeroom (live demo)").first()).toBeVisible();

  // Scanning the same badge again must not produce a second record.
  await page.getByLabel("Student number").fill("2024-1001");
  await page.getByRole("button", { name: "Record" }).click();
  await expect(page.getByText("Already recorded today")).toBeVisible();
});

test("an unknown number and a hostile payload are both refused", async ({ page }) => {
  await page.goto("/scan");

  await page.getByLabel("Student number").fill("9999-0000");
  await page.getByRole("button", { name: "Record" }).click();
  await expect(page.getByText("is not on any roster")).toBeVisible();

  await page.getByLabel("Student number").fill("<script>alert(1)</script>");
  await page.getByRole("button", { name: "Record" }).click();
  await expect(page.getByText("That QR code is not a student ID.")).toBeVisible();
});

test("a section can be created, renamed, and removed", async ({ page }) => {
  await page.goto("/sections");

  await page.getByRole("button", { name: "New section" }).first().click();
  await page.getByLabel("Section name").fill("Grade 10 - Bonifacio");
  await page.getByRole("button", { name: "Create section" }).click();

  // Scoped to main: the success toast repeats the name, which would match two nodes.
  await expect(page.locator("main").getByRole("link", { name: /Grade 10 - Bonifacio/ })).toBeVisible();

  // A duplicate name is refused, with the reason shown on the field itself.
  await page.getByRole("button", { name: "New section" }).first().click();
  await page.getByLabel("Section name").fill("Grade 10 - Bonifacio");
  await page.getByRole("button", { name: "Create section" }).click();
  await expect(page.locator("dialog").getByRole("alert")).toContainText("already exists");
  await page.getByRole("button", { name: "Cancel" }).click();

  // Renaming keeps it in the list under the new name.
  await page
    .locator("main")
    .locator("li", { hasText: "Grade 10 - Bonifacio" })
    .getByRole("button", { name: "Rename" })
    .click();
  await page.getByLabel("Section name").fill("Grade 10 - Luna");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.locator("main").getByRole("link", { name: /Grade 10 - Luna/ })).toBeVisible();

  // Removing takes it out of the list.
  await page
    .locator("main")
    .locator("li", { hasText: "Grade 10 - Luna" })
    .getByRole("button", { name: "Remove" })
    .click();
  await page.getByRole("button", { name: "Remove section" }).click();
  await expect(page.locator("main").getByRole("link", { name: /Grade 10 - Luna/ })).toHaveCount(0);
});

test("a student can be added and given a QR code", async ({ page }) => {
  await page.goto("/sections");
  await page.getByText("Grade 11 - Rizal").first().click();

  await page.getByRole("button", { name: "Add student" }).first().click();
  await page.getByLabel("Last name").fill("Testerson");
  await page.getByLabel("First name").fill("Terry");
  await page.getByLabel("Student number").fill("2024-9001");
  await page.getByRole("button", { name: "Add student" }).last().click();

  await expect(page.locator("main").getByText("Testerson, Terry", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: /Show QR code for Testerson/ }).click();
  await expect(page.getByAltText(/QR code encoding student number 2024-9001/)).toBeVisible();
});

test("a report renders and exports a CSV", async ({ page }) => {
  await page.goto("/reports");

  await expect(page.getByRole("table")).toBeVisible();
  await expect(page.getByRole("columnheader", { name: "Present" })).toBeVisible();

  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export CSV" }).click();
  const file = await download;

  expect(file.suggestedFilename()).toMatch(/^attendance-Grade-11-Rizal-.*\.csv$/);
});

test("backup and restore round-trips through the data screen", async ({ page }) => {
  await page.goto("/data");

  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Download backup" }).click();
  const file = await download;
  expect(file.suggestedFilename()).toMatch(/^attendance-backup-\d{4}-\d{2}-\d{2}\.json$/);

  await page.getByRole("button", { name: "Erase all data" }).click();
  await page.getByRole("button", { name: "Erase everything" }).click();
  await expect(page.getByText("All data erased.")).toBeVisible();
});

test("navigation works on a phone-sized viewport", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/");

  for (const label of ["Scan", "Class", "Sched", "Report", "Data"]) {
    await page.getByRole("link", { name: label, exact: true }).click();
    await expect(page.locator("main h1")).toBeVisible();

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow, `horizontal overflow on ${label}`).toBeLessThanOrEqual(0);
  }
});
