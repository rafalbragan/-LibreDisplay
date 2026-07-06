# LibreDisplay

LibreDisplay uses a **non-official** LibreLinkUp/LibreView API integration. API responses can change over time, so the client includes defensive parsing, region discovery, and safe diagnostics.

## Environment configuration

Set these variables before building (CI secrets recommended):

- `LIBRE_API_BASE_URL` - optional override, default `https://api-eu.libreview.io`
- `LIBRE_LINKUP_VERSION` - optional, default `4.16.0`
- `LIBRE_PATIENT_ID` - optional patient selection when multiple connections exist

Example (PowerShell):

```powershell
$env:LIBRE_API_BASE_URL = "https://api.libreview.io"
$env:LIBRE_LINKUP_VERSION = "4.16.0"
$env:LIBRE_PATIENT_ID = "patient-id-from-connections"
.\gradlew.bat assembleDebug
```

## Region and login behavior

- Settings support region mode: `AUTO`, `GLOBAL`, `EU`, `EU2`, `DE`, `CUSTOM`.
- In `AUTO`, login starts on `https://api.libreview.io/llu/auth/login` and follows **one** regional redirect (`data.redirect=true`, `data.region=...`).
- After successful login, the detected regional base URL is reused for connections/graph calls.
- A "Testuj polaczenie" action in Settings validates login + connections without starting automatic monitoring.

## Security and diagnostics notes

- Credentials should come from local environment or CI secret storage.
- Diagnostic logs mask e-mail, password, and bearer token values.
- Full login JSON payload is never written to logs.
- Login diagnostics include only safe metadata (masked email, lengths, whitespace flags).

## API behavior notes

- Login token path is read from `data.authTicket.token`.
- Connections are read from `/llu/connections` using `Authorization: Bearer <JWT>`.
- Graph history is read from the graph endpoint and may be shorter than 24h.
- The app logs history count and oldest/newest timestamp to show the real available range.
