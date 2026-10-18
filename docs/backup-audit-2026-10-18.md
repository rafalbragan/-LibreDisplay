# Backup Audit - 2026-10-18

## Scope and constraints
- Read-only audit only (no restore/import to phone).
- No health payload data copied to repository logs.

## Source and secured copy
- Source file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\docs\LibreCare-live-backup-1787331718332.json`
- Secured copy (outside Git repo): `C:\Users\SG0216827\LibreCare-backup-vault\LibreCare-live-backup-1787331718332-copy-20261018-030233.json`
- SHA-256 source: `BB1A8AAF691392625944E6A84C5E161679BC3D0E0957284EF99ACF707536167C`
- SHA-256 copy: `BB1A8AAF691392625944E6A84C5E161679BC3D0E0957284EF99ACF707536167C`
- Hash match: `YES`

## Metadata inspection result
- Detected format: `v1` (legacy compact-key backup)
- Schema version: `1`
- App version in backup: `2.2.2`
- Backup created at: `2026-08-21T17:02:03Z`
- Persons count: `2`
- Readings count: `426`
- Patient settings count: `0`
- Reading date range (UTC):
  - From: `2026-07-28T07:01:16Z`
  - To: `2026-08-21T16:56:39Z`

## Integrity checks
- Invalid reading rows: `0`
- Missing person references in readings: `0`
- Unique person IDs: `2`
- Duplicate person IDs: `0`

## Assessment for currently lost data
- This backup is internally consistent and usable as an archival source.
- It does **not** include data from yesterday/today (relative to 2026-10-18), because the latest reading timestamp is `2026-08-21T16:56:39Z`.
- Conclusion: backup can help recover historical data up to 2026-08-21, but cannot recover newer missing records after that date.

