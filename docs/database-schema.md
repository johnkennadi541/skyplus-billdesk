# Database schema (design only)

Status: **design only**. Nothing here is implemented yet - Room arrives in Step 3.
This page exists so the models, the billing engine and the UI are built against a
fixed shape.

Engine: Room over SQLite, on the device. No backend in v0.1.

## Tables

| Table | Purpose | Key fields |
|---|---|---|
| `users` | The profiles that can operate the till | `id`, `role`, `authMethod`, `googleSubject`, `email`, `displayName`, `pinHash`, `pinSalt`, `failedAttempts`, `lockedUntil` |
| `categories` | Starter, Main Course, Beverages, Combos | `id`, `name`, `sortOrder`, `isActive` |
| `menu_items` | Sellable items and prices | `id`, `code`, `name`, `categoryId`, `pricePaise`, `taxPercent`, `isActive` |
| `orders` | A live order before it becomes a bill | `id`, `orderType`, `tableNumber`, `openedAt`, `closedAt` |
| `order_items` | Lines of a live order | `id`, `orderId`, `menuItemId`, `quantity`, `note` |
| `bills` | Saved bills, never deleted | `id`, `billNumber`, `createdAt`, `orderType`, `tableNumber`, `subtotalPaise`, `taxPaise`, `serviceChargePaise`, `discountPaise`, `grandTotalPaise`, `paymentMode`, `createdByRole`, `discountAppliedBy`, `voidedBy`, `voidedAt` |
| `bill_items` | Frozen copy of each sold line | `id`, `billId`, `menuItemId`, `nameSnapshot`, `unitPricePaise`, `quantity`, `note` |
| `payments` | How the money came in | `id`, `billId`, `mode`, `amountPaise`, `paidAt` |
| `inventory_items` | Tracked stock | `id`, `name`, `unit`, `quantityBase`, `reorderLevelBase`, `costPerBasePaise`, `isActive` |
| `stock_movements` | Every change to stock | `id`, `inventoryItemId`, `type`, `quantityBase`, `atEpochMillis`, `byRole`, `note` |
| `settings` | Restaurant name, tax %, service charge, bill header / footer, landing profile | `key`, `value` |
| `audit_log` | Anything sensitive that is not on a bill | `id`, `at`, `role`, `action`, `detail` |

Planned but not built: `recipes` (`menuItemId`, `inventoryItemId`, `quantityBase`)
for automatic deduction, if option A is chosen later.

## Rules

1. **Money is `Long` paise** in every column. No `REAL`, no `DOUBLE`.
2. **Stock quantities are `Long` base units** - grams, millilitres, pieces. Never
   a floating point number.
3. **Bills are immutable** apart from voiding. Editing a bill creates a new
   version or a void plus a re-issue, never a silent overwrite.
4. **`bill_items` copies name and price** at sale time. A later menu price change
   must never alter a past bill.
5. **`billNumber` is unique** and sequential per device.
6. **PINs are stored as a hash with a salt**, never in clear text.
   A Google account is only accepted if it is on the device's admin allow-list;
   a valid Google login alone never grants admin.
7. **Deletes are soft** (`isActive = false`) for categories, menu items and stock
   items, so old bills and old movements still resolve.
8. **Stock never changes without a `stock_movements` row.** The item's
   `quantityBase` is the running total; the movements are the history. Both are
   written in one transaction.
9. **Stock may not go below zero.** A correction is a movement, not a negative
   quantity.
10. **Indices**: `menu_items(code)`, `bills(createdAt)`, `bill_items(billId)`,
    `orders(closedAt)`, `stock_movements(inventoryItemId, atEpochMillis)`.

## Migrations

Schema version starts at 1. Every later change ships a written Room migration and
a test that opens an old database and upgrades it. Destructive fallback
(`fallbackToDestructiveMigration`) is not acceptable once real bills exist.
