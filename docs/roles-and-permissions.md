# Roles and permissions

There is exactly one app and one database. Cashier and Admin are profiles inside
it, not separate apps.

The table below is generated from `app/src/main/kotlin/com/skyplus/billdesk/auth/PermissionMatrix.kt`.
That file is the source of truth; this page must match it.

| Permission | What it controls | Cashier | Admin |
|---|---|---|---|
| `BILLING_CART` | Billing & cart - take order, make bill, print / share | Yes | Yes |
| `ORDER_TYPE_AND_KOT` | Order type (dine-in / takeaway / delivery) and KOT | Yes | Yes |
| `DISCOUNT_WITHIN_LIMIT` | Discount up to the cashier limit | Yes | Yes |
| `DISCOUNT_ABOVE_LIMIT` | Discount above the cashier limit | - | Yes |
| `VOID_REFUND_EDIT_BILL` | Void / refund / edit a saved bill | - | Yes |
| `MENU_VIEW` | See menu, categories and prices | Yes | Yes |
| `MENU_EDIT` | Add / edit / delete items, prices and categories | - | Yes |
| `TAX_SETTINGS_VIEW` | See tax %, service charge and receipt settings | Yes | Yes |
| `TAX_SETTINGS_EDIT` | Change tax %, service charge, restaurant name, header / footer | - | Yes |
| `SALES_REPORT_OWN_SHIFT` | Own-shift / today's basic bill list | Yes | Yes |
| `SALES_REPORT_FULL` | Full sales report, item-wise sales, payment summary | - | Yes |
| `PROFILE_SWITCH` | Switch between cashier and admin profiles | Yes | Yes |
| `RESET_CASHIER_PIN` | Reset a cashier PIN | - | Yes |
| `INVENTORY_VIEW` | See stock levels, low-stock list and stock value | Yes | Yes |
| `INVENTORY_EDIT` | Add / edit / deactivate stock items, units, reorder levels and cost | - | Yes |
| `STOCK_MOVEMENT` | Record a purchase, usage, wastage or stock adjustment | - | Yes |

## Landing profile

- The owner's device opens in **Admin** by default.
- The landing profile is stored per device, so a cashier's tablet can open in Cashier.
- Every fresh app start asks for that profile's PIN once. There is no "stay admin
  all day" option: an unattended admin till can change prices and void bills.
- Switching profile never closes an open bill.

## Discount policy

- A cashier may apply a discount up to **10%** with no extra approval.
- Above 10%, an admin PIN is required at the moment of billing, and the bill
  records `discountAppliedBy = ADMIN`.
- Admin has no cap and never needs a PIN for a discount.

The 10% figure is business policy and is a single constant
(`PermissionGuard.CASHIER_MAX_DISCOUNT_PERCENT`). Confirm the real number with
the restaurant before release.

## Inventory access

- Cashier: may look at stock levels and the low-stock list. Cannot change anything.
- Admin: adds and edits stock items, records purchases, usage, wastage and
  adjustments.
- If the kitchen should be able to record wastage from a cashier profile, that is
  one line in `PermissionMatrix` - decide before release, not after.

## Audit trail

Every saved bill carries:

| Field | Meaning |
|---|---|
| `createdByRole` | Which profile created the bill |
| `discountAppliedBy` | Which profile approved the discount, if any |
| `voidedBy` | Which profile voided the bill |
| `voidedAtEpochMillis` | When it was voided; null means the bill stands |

Every stock movement carries `byRole`, `atEpochMillis` and a note, so a stock
figure can always be traced back to who entered it and when.

A bill is never deleted. Voiding keeps the row and the reason, so the day's
accounts can always be reconstructed.

## Login rules

- Cashier: 4-digit PIN, fast to type while busy, with a delay after repeated failures.
- Admin: Sign in with Google. On a device without Google Play services (many POS
  terminals), admin falls back to a PIN, with the same lockout.
- Switching back to Cashier from Admin is also gated, so a cashier cannot walk
  into the admin session someone left open.
- Full detail: `docs/auth-and-accounts.md`.
