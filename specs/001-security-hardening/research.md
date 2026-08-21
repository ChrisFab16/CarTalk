# Research: Security Hardening

## Decision 1: OkHttp logging policy

**Decision**: Release builds use `HttpLoggingInterceptor.Level.NONE`. Debug builds may use `HEADERS` or `BASIC` with a redacting interceptor that replaces `x-api-key` header values with `***`.

**Rationale**: BODY logging was logging the full API key and conversation payloads to Logcat (C1).

**Alternatives considered**: Always NONE (loses debug utility); custom logger only in debug with redaction (chosen).

## Decision 2: Fail-closed EncryptedSharedPreferences

**Decision**: Remove the catch-all fallback to `cartalk_prefs`. If MasterKey / EncryptedSharedPreferences creation fails, PreferencesManager exposes `isSecureStorageAvailable == false`, `setApiKey` throws or returns failure, and Settings shows an error toast/dialog.

**Rationale**: Silent plaintext storage (H1) is worse than refusing to save.

**Alternatives considered**: Retry once; migrate existing plaintext keys then wipe (nice-to-have if file exists — wipe `cartalk_prefs` key on startup if present).

## Decision 3: Backup exclusions

**Decision**: Keep `allowBackup` behavior controlled via `android:fullBackupContent` + `android:dataExtractionRules` XML excluding:

- `sharedpref` `cartalk_secure_prefs`
- `sharedpref` `cartalk_prefs`
- `database` `cartalk_database`

**Rationale**: Excluding sensitive files is preferable to disabling all backup for UX; minSdk 29 supports data extraction rules.

**Alternatives considered**: `allowBackup="false"` (simpler, more blunt).

## Decision 4: HostValidator

**Decision**: Release → `HostValidator.Builder(applicationContext).addAllowedHosts(R.array.hosts_allowlist_sample)` or Car App Library recommended Play hosts builder if available in 1.4; else create allowlist array resource. Debug → ALLOW_ALL behind `BuildConfig.DEBUG`.

**Rationale**: H3; ALLOW_ALL is for development only.

**Note**: Exact API: `HostValidator.createAllowListValidator` / Builder with `androidx.car.app.R.array.hosts_allowlist_sample` — verify at implement time against car-app 1.4.

## Decision 5: Key UI

**Decision**: Never `setText(existing)` with the full key. Status text: "Configured" / "Not configured" only on phone and car.

## Decision 6: Unsplash URL

**Decision**: Build `https://source.unsplash.com/400x300/?${URLEncoder.encode(query, UTF_8)}` after stripping characters outside a safe set OR encode fully; do not interpolate raw LLM text. Keep host fixed.

## Decision 7: Car delete confirm

**Decision**: Use a second-step car template (MessageTemplate or Pane with Confirm/Cancel actions) before calling `deleteDocument`.

## Decision 8: Posture doc

**Decision**: Add `docs/security.md` listing mitigations and accepted gaps (cloud STT, Claude API sees chat content, no SQLCipher in this pass).
