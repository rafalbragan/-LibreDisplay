# LibreCare Navigation Matrix

## Routing model

LibreCare uses a custom stack-based navigation state:

- `AppNavigationState`
- `AppScreen`
- `navigateTo(...)`
- `navigateBack()`
- `switchToTopLevel(...)`

## Top-level destinations

| Destination | Depth | Typical entry points | Back behavior |
| --- | --- | --- | --- |
| `Start` | 1 | App launch in first-run / setup mode | Finishes app only if explicitly confirmed |
| `Monitoring` | 1 | App launch after successful login / live mode | Back opens exit confirmation |
| `Analytics` | 1 | Bottom navigation / Home action | Back returns to previous top-level screen |
| `Settings` | 1 or 2 | Bottom navigation / login-only flow | Back returns to previous screen or exits login flow |

## Nested destinations

| Destination | Parent | Entry path | Notes |
| --- | --- | --- | --- |
| `SettingsTargetRange` | `Settings` | Settings → Monitoring → Target range | Modifies target-range preferences |
| `SettingsHomeMetrics` | `Settings` | Settings → Monitoring → Home metrics | Controls Home metric visibility/order |
| `SettingsHbA1c` | `Settings` | Settings → Monitoring → HbA1c | HbA1c-related settings |
| `SettingsAccount` | `Settings` | Settings → LibreLinkUp | Login / account connection |
| `PrivacyData` | `Settings` | Settings → Privacy & data | Privacy / backup / reset flows |
| `Statistics` | `Settings`, `PrivacyData`, `About` | Settings → Statistics | Shared diagnostics view |
| `About` | `Settings` | Settings → About | App information |
| `Diagnostics` | `Settings`, `Monitoring` | Settings → Advanced / Monitoring → Diagnostics | Log / audit tools |
| `Retention` | `Settings`, `PrivacyData` | Settings → Data & privacy → Retention | Data retention policy |
| `Polling` | `Settings` | Settings → Monitoring → Sync | Polling frequency and battery impact |

## Max-depth route examples

1. `Start -> Settings`
2. `Monitoring -> Settings -> SettingsTargetRange`
3. `Monitoring -> Settings -> SettingsHomeMetrics`
4. `Monitoring -> Settings -> SettingsHbA1c`
5. `Monitoring -> Settings -> SettingsAccount`
6. `Monitoring -> Settings -> PrivacyData -> Statistics`
7. `Monitoring -> Settings -> PrivacyData -> Retention`
8. `Monitoring -> Settings -> About -> Statistics`

## Test coverage target

Automated navigation tests should verify:

- top-level selection never creates duplicates
- back navigation returns to the real parent screen
- max-depth routes remain reachable
- login-only flow keeps `Start -> Settings`
- `Monitoring` remains the canonical root for live mode
- route transitions preserve the selected person and the selected range
