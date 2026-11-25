# Features and roadmap

Source: the Restaurant Billing App GitHub Project Plan (sections 1 and 6), plus
two additions requested later: Admin landing profile and inventory management.
Status legend: Not started / In progress / Done / Blocked.

## Phase 1 - MVP

| Feature | Detail | Status |
|---|---|---|
| Menu management | Add / edit / delete items, categories (Starter, Main Course, Beverages, Combos) | Not started (model done) |
| Search and quick add | Find an item by name or code, add to order fast | Not started (model done) |
| Cart / order screen | Quantity +/-, per-item notes (less spicy, no onion), running total | Not started |
| Tax and discount | GST / VAT or service charge, discount as percent or amount | Not started (Step 2) |
| Bill generation | Clear summary, print / PDF preview, share on WhatsApp | Not started |
| Local storage | Offline database on the device | Not started (Step 3) |

## Phase 2 - Enhancement

| Feature | Detail | Status |
|---|---|---|
| Order type | Dine-in (table no.), Takeaway, Delivery | Not started (model done) |
| KOT | Kitchen ticket with item, quantity and notes, no prices | Not started |
| Payment modes | Cash, Card, UPI / Online | Not started (model done) |
| Daily sales summary | Total sales, order count, best-selling items | Not started |

## Added later - Admin landing and inventory

| Feature | Detail | Status |
|---|---|---|
| Admin landing profile | App opens in Admin on the owner's device, per-device setting, login on every launch | Not started (policy + model done) |
| Admin Google login | Sign in with Google for the admin profile, PIN fallback on devices without Play services | Not started (policy done) |
| Inventory list | Stock items with unit, quantity, reorder level, cost per unit; low stock highlighted | Not started (model done) |
| Stock movements | Record purchase, usage, wastage (reason required) and adjustment | Not started (model done) |
| Stock value and wastage report | Value of stock on hand, wastage for the day / month | Not started |

**Inventory mode: manual (option B).** Nothing is deducted automatically when a
bill is made. Stock changes only when the admin records a movement. Recipe-based
automatic deduction (option A) is a later decision and needs a `recipes` table.

## Build order

| Week | Goal | Tasks |
|---|---|---|
| 1 | Models and roles | Repo, README, .gitignore, UserRole, MenuItem, OrderItem, Bill, permission guards |
| 2 | Login and billing engine | Admin Google sign-in, cashier PIN, Play services fallback, landing profile, subtotal, tax, discount, grand total, edge case tests, CI build |
| 3 | Cashier and Admin UI | Menu filter, search, live cart, bill summary; menu CRUD, tax settings, daily summary |
| 4 | Persistence, KOT, release | Save bills and settings, bill history, KOT preview, print / PDF / share, v0.1.0 tag |
| 5 | Inventory | Room tables, stock list, purchase / usage / wastage entry, low-stock list, stock value report |
| 6 | Supabase Auth | Admin Google login through Supabase, real ID token verification, session refresh |
| 7 | Two-way sync | Pending queue, idempotent device-generated ids, conflict rules, row level security, sync status screen |

## Deliberately out of scope for v0.1

- Cloud sync and multi-device bills
- Online ordering / delivery partner integration
- Recipe-based automatic stock deduction
- Multi-outlet management
