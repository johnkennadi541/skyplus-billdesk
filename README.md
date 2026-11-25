# SkyPlus BillDesk

Restaurant billing app for Android - one APK for a phone, a tablet and an Android POS terminal.

Two operating profiles in a single app:

- **Cashier** - fast billing, cart, KOT, limited discount.
- **Admin** - menu and pricing, tax and receipt settings, unlimited discount, void / refund, full reports.

## Identity (do not change casually)

| Item | Value |
|---|---|
| App name | SkyPlus BillDesk |
| Application ID | `com.skyplus.billdesk` |
| Package root | `com.skyplus.billdesk` |
| minSdk / targetSdk | 23 / 35 |
| Version | 0.1.0 (versionCode 1) |

The application ID is permanent after the first install. Changing it later forces every
user to uninstall and reinstall, and loses local data.

## Stack

- Kotlin + Jetpack Compose
- Room (local, offline-first) - added in Step 3
- No backend in v0.1. The device database is the source of truth.

## Repository layout

```
skyplus-billdesk/
├── app/
│   └── src/
│       ├── main/kotlin/com/skyplus/billdesk/
│       │   ├── model/      UserRole, Permission, Category, MenuItem, OrderItem, Bill, Money
│       │   ├── auth/       PermissionMatrix, PermissionGuard
│       │   └── printing/   BillPrinter interface + preview stub
│       └── test/kotlin/com/skyplus/billdesk/   unit tests
├── docs/
│   ├── features.md
│   ├── roles-and-permissions.md
│   └── database-schema.md
└── README.md
```

## Status

| Step | Scope | Status |
|---|---|---|
| 1 | Repository, docs, models, permission guard, tests | Done |
| 2 | Profile switch + PIN, billing engine (tax, discount, totals) | Not started |
| 3 | Cashier UI, Admin UI, Room persistence | Not started |
| 4 | KOT, bill history, print / PDF / share, v0.1.0 release | Not started |

## Build

Not verified yet - the project has no Gradle wrapper and has not been compiled.
`./gradlew assembleDebug` is added in Step 2 once the build environment is confirmed.

## POS notes

- Built-in POS printers are **not** reachable through the Android print framework.
  A per-brand ESC/POS adapter (Sunmi, iMin, ...) is added behind the `BillPrinter`
  interface once the exact terminal model is known.
- POS terminals often run Android 7-10 with no Google Play: install by sideloading the APK.
- Layouts must work in landscape as well as portrait from the first screen.
