# Step 2 implementation notes

This change adds station-grouped local KOT rendering (existing `KotBuilder`), pure ETA calculation, deterministic event/cancellation projection, a 10-second table lease policy, Room-backed print queue and printer profile persistence (limited to two configured profiles), retry statuses, paise-based tender validation, sales/cancellation summaries, receipt ETA formatting and Material 3 composables.

The ESC/POS engine chunks at 128 bytes and paces writes. TCP writes all chunks over one socket using port 9100 by default. Bluetooth SPP and USB are represented by adapter interfaces and can be injected, but no concrete Android `BluetoothSocket`/`UsbManager` implementation or physical printer behavior has been implemented/verified here. Successful bytes written are reported only as `Sent to Printer`; retries are at-least-once and a partial transmission retry may duplicate a ticket.

Room schema advances from v1 to v2 via `MIGRATION_1_2` (no destructive migration), adding durable print jobs, printer profiles and leases. Existing domain models and `BillingEngine` are untouched. Billing screen accepts totals from the existing billing engine; GST/discount arithmetic is not reimplemented.

Build/tests could not be run through the available GitHub repository tools; verify locally with `./gradlew test` and `./gradlew assembleDebug` after configuring Android SDK/Gradle.
