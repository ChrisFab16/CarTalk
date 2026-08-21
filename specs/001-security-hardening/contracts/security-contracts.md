# Security Contracts

## C-LOG-001 — HTTP logging

- **Release**: `HttpLoggingInterceptor` level MUST be `NONE` (or interceptor omitted).
- **Debug**: If logging is enabled, any logged `x-api-key` header value MUST be redacted to a constant placeholder (e.g. `***`).
- **Forbidden**: `Level.BODY` in release; logging raw API key strings.

## C-STORE-001 — API key persistence

- `PreferencesManager` MUST NOT call `getSharedPreferences("cartalk_prefs", …)` as a fallback for API key storage.
- If encrypted prefs cannot be created: `hasApiKey()` is false; `setApiKey` fails; callers show error.
- On init, if plaintext `cartalk_prefs` contains `claude_api_key`, remove it.

## C-BACKUP-001 — Backup / extraction

Manifest references:

- `android:fullBackupContent="@xml/backup_rules"`
- `android:dataExtractionRules="@xml/data_extraction_rules"`

Both XML files MUST exclude shared prefs `cartalk_secure_prefs`, `cartalk_prefs`, and database `cartalk_database`.

## C-HOST-001 — Car App host validation

```text
IF BuildConfig.DEBUG THEN ALLOW_ALL MAY be used
ELSE HostValidator MUST NOT be ALLOW_ALL_HOSTS_VALIDATOR
```

## C-UI-001 — Key presentation

- Settings dialog MUST NOT prefill the saved secret.
- Status strings MUST NOT include any substring of the key.
- Car Setup screen MUST NOT include any substring of the key.

## C-URL-001 — Visual image load

- Image request URL host MUST be `source.unsplash.com` (HTTPS).
- Query MUST be `URLEncoder.encode(searchQuery, UTF_8)`.

## C-DELETE-001 — Car document delete

- Invoking Delete MUST show a confirmation template with Cancel / Confirm.
- `deleteDocument` runs only after Confirm.
