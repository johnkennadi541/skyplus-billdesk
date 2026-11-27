# Phase 1 local event sync and daily backup

## Local two-device sync

`LocalWifiEventSync` sends the existing `event_log` rows over an explicitly opened TCP connection. The host calls `serveOnce(ServerSocket)` on a background executor; the peer calls `syncWith(hostAddress, port)`. Exchange is idempotent and preserves per-device `(deviceId, seq)` order. Reusing an event id or origin sequence with different contents fails closed; no existing history is overwritten or deleted. Incoming cancellation events remain in `event_log`, update the local order projection when its order exists, and can be shown through `OrderCancelledBanner` in red using `EventSyncDao.cancellations()`.

For a no-router setup, enable one Android device's system hotspot, connect the second device to that hotspot, then use the hotspot device's reachable local IP and the same configured TCP port. Android hotspot address and client isolation vary by manufacturer/version; discovery and hotspot enabling are deliberately left to Android system settings. This is a two-device, manually initiated Phase 1 link, not mesh sync.

The existing Room table-edit lease and 10-second rule are unchanged in storage and are not represented as business events. The same-device holder can renew; another device cannot acquire the local lease until it expires. As with any disconnected pair of independent SQLite databases, no implementation can guarantee mutual exclusion during a network partition; connect peers before editing shared tables and treat the lease as a short best-effort coordination window, not a distributed consensus lock.

## Daily backup

`DailyEventBackupScheduler` enqueues unique, periodic WorkManager work without a network constraint. It stores a versioned JSON event-log snapshot at `filesDir/backups/daily-events-YYYY-MM-DD.json` and uses an atomic temporary file replacement. `EventBackupSerializer.writeTo()` can export the same deterministic snapshot to an app-selected `OutputStream` (for example, Storage Access Framework). `OptionalEventBackupHook` is the extension boundary for future cloud/SQL storage; it is not required for billing/KOT.

Backups contain append-only events and original payload strings, not PINs or auth tokens. Restoring/replaying a complete database is not yet implemented; retain the backup files independently from the device.
