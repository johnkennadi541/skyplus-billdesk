# Milestone 1: package structure and local schema

## Package structure

Keep a single `:app` Gradle module for this MVP; splitting modules adds build and dependency overhead without helping the offline data boundary yet.

```text
com.skyplus.billdesk/
├── model/       Existing shared domain models (MenuItem, Category, OrderItem, Bill, Money); do not duplicate
├── billing/     Existing pure billing engine (unchanged in this milestone)
├── data/
│   ├── entity/  Room rows / materialized views (separate from existing domain models)
│   ├── dao/     Room DAO contracts and atomic local order operations
│   ├── database/ RoomDatabase, WAL setup and versioned migrations
│   └── repository/ Offline order/KOT repository boundary
├── kitchen/     Station/language, order/KOT domain models and deterministic local KOT projection
└── ui/          Existing login/theme; future screens under ui/menu, ui/order, ui/kot
```

## Room schema (version 1)

| Table | Primary/index keys | Purpose |
|---|---|---|
| `categories` | id; unique normalized name | Local menu categories; soft-active flag |
| `menu_items` | id; unique code; category index | Name, `pricePaise: Long`, tax percent, active flag |
| `kitchen_stations` | stationId; language index | Station name, configured `TAMIL/HINDI/ENGLISH/NEPALI` language |
| `station_menu_mapping` | (stationId, menuItemId) | Explicit station-to-menu assignment; cascading references are intentionally avoided in the append-only history |
| `menu_translations` | (menuItemId, language) | Optional offline saved menu name translations |
| `orders` | orderId; status/type/time indexes | Dine-in or parcel (`TAKEAWAY` domain value), table label, open/close timestamps and lifecycle status |
| `order_items` | lineId; order index | Immutable-at-sale snapshots for name, price paise, tax, station; current quantity/note projection |
| `kot_records` | kotId; order/station index | Station/language snapshot, created timestamp, ticket status |
| `kot_lines` | lineId; KOT index | Printed-name snapshot, quantity and note snapshot |
| `event_log` | eventId; unique (deviceId, seq); type/time index | Append-only local business history; explicit `ORDER_CANCELLED` event |

Money is always integer paise (`Long`); existing `Money` remains the conversion/formatting API. Events store versioned JSON payloads; cancellation is a new event, never a hard delete. Order/KOT rows are query-friendly materialized state, while event history remains append-only. A later schema change must add an explicit Room `Migration`; destructive fallback is prohibited once real till data exists.

Room is opened with `JournalMode.WRITE_AHEAD_LOGGING`. This only enables SQLite WAL; offline operation is the default and does not imply sync/printer functionality.
