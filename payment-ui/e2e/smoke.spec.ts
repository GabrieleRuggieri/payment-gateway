import { expect, test } from '@playwright/test';

test.describe('payment-ui smoke', () => {
  test('loads brand hero, skip-link and lazy collection', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText('Payment Gateway', { exact: true }).first()).toBeVisible();
    await expect(page.getByRole('heading', { name: /Authorize\. Capture\. Settle\./i })).toBeVisible();
    await expect(page.getByLabel('Amount')).toBeVisible();
    await expect(page.getByLabel('Currency')).toBeVisible();

    const skip = page.getByRole('link', { name: /Skip to payment composer/i });
    await skip.focus();
    await expect(skip).toBeFocused();

    await expect(page.getByRole('heading', { name: 'Payment Gateway API' })).toBeVisible({
      timeout: 15_000,
    });
  });

  test('example amount updates composer', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: '€49.99' }).click();
    await expect(page.getByLabel('Amount')).toHaveValue('49.99');
    await expect(page.getByLabel('Currency')).toHaveValue('EUR');
  });
});
