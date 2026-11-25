# Database schema (design only)

Status: **design only**. Nothing here is implemented yet - Room arrives in Step 3.
This page exists so the models, the billing engine and the UI are built against a
fixed shape.

Engine: Room over SQLite, on the device. No backend in v0.1.

## Tables

| Table | Purpose | Key fields |
|---|---|---|
| `users` | The profiles that can operate the till | `id`, `role`, `displayName`, `pinHash`, `pinSalt`, `failedAttempts`, `lockedUntil` |
| `categories` | Starter, Main Course, Beverages, Combos | `id`, `name`, `sortOrder`, `isActive` |
| `menu_items` | Sellable items and prices | `id`, `code`, `name`, `categoryId`, `pricePaise`, `taxPercent`, `isActive` |
| `orders` | A live order before it becomes a bill | `id`, `orderType`, `tableNumber`, `openedAt`, `closedAt` |
| `order_items` | Lines of a live order | `id`, `orderId`, `menuItemId`, `quantity`, `note` |
| `bills` | Saved bills, never deleted | `id`, `billNumber`, `createdAt`, `orderType`, `tableNumber`, `subtotalPaise`, `taxPaise`, `serviceChargePaise`, `discountPaise`, `grandTotalPaise`, `paymentMode`, `createdByRole`, `discountAppliedBy`, `voidedBy`, `voidedAt` |
| `bill_items` | Frozen copy of each sold line | `id`, `billId`, `menuItemId`, `nameSnapshot`, `unitPricePaise`, `quantity`, `note` |
| `payments` | How the money came in | `id`, `billId`, `mode`, `amountPaise`, `paidAt` |
| `settings` | Restaurant name, tax %, service charge, bill header / footer | `key`, `value` |
| `audit_log` | Anything sensitive that is not on a bill | `id`, `at`, `role`, `action`, `detail` |

## Rules

1. **Money is `Long` paise** in every column. No `REAL`, no `DOUBLE`.
2. **Bills are immutable** apart from voiding. Editing a bill creates a new
   version or a void plus a re-issue, never a silent overwrite.
3. **`bill_items` copies name and price** at sale time. A later menu price change
   must never alter a past bill.
4. **`billNumber` is unique** and sequential per device.
5. **PINs are stored as a hash with a salt**, never in clear text.
6. **Deletes are soft** (`isActive = false`) for categories and menu items, so old
   bills still resolve their items.
7. **Indices**: `menu_items(code)`, `bills(createdAt)`, `bill_items(billId)`,
   `orders(closedAt)`.

## Migrations

Schema version starts at 1. Every later change ships a written Room migration and
a test that opens an old database and upgrades it. Destructive fallback
(`fallbackToDestructiveMigration`) is not acceptable once real bills exist.
