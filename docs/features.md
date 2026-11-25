# Features and roadmap

Source: the Restaurant Billing App GitHub Project Plan, sections 1 and 6.
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

## Build order

| Week | Goal | Tasks |
|---|---|---|
| 1 | Models and roles | Repo, README, .gitignore, UserRole, MenuItem, OrderItem, Bill, permission guards |
| 2 | Profile switch and billing engine | PIN verification, subtotal, tax, discount, grand total, edge case tests |
| 3 | Cashier and Admin UI | Menu filter, search, live cart, bill summary; menu CRUD, tax settings, daily summary |
| 4 | Persistence, KOT, release | Save bills and settings, bill history, KOT preview, print / PDF / share, v0.1.0 tag |

## Deliberately out of scope for v0.1

- Cloud sync and multi-device bills
- Online ordering / delivery partner integration
- Inventory and stock deduction
- Multi-outlet management
