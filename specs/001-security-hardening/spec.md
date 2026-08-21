# Feature Specification: Security Hardening

**Feature Branch**: `main` (publish via `pr/security-hardening`)

**Created**: 2026-08-21

**Status**: Draft

**Input**: Remediate adversarial review findings C1, H1–H3, M1–M5 for CarTalk (API key leak via Logcat, plaintext prefs fallback, Auto Backup exposure, ALLOW_ALL car host validator, key UI exposure, Room backup, Unsplash URL construction, car delete without confirm).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - API traffic does not leak credentials (Priority: P1)

As a CarTalk user who configured an Anthropic API key, I need network logging never to expose my key or full request bodies in release builds so that ADB/Logcat cannot steal credentials.

**Why this priority**: Direct credential exfiltration (C1) — highest severity.

**Independent Test**: Build release (or debug with release logging policy), send one chat message, confirm Logcat contains no `x-api-key`, `sk-ant-`, or `sk-` API key values.

**Acceptance Scenarios**:

1. **Given** a configured API key and release build, **When** the app calls Claude API, **Then** OkHttp logging is `NONE` (or equivalent non-BODY) and no secret headers appear in Logcat.
2. **Given** a debug build with optional HTTP logging enabled, **When** requests are logged, **Then** `x-api-key` values are redacted.

---

### User Story 2 - API key storage fails closed (Priority: P1)

As a user saving an API key, I need encrypted storage or a clear failure — never silent plaintext SharedPreferences — so my key is not left unencrypted on disk (H1).

**Why this priority**: Fail-open storage defeats EncryptedSharedPreferences.

**Independent Test**: Force or simulate encryption init failure; assert key is not written to `cartalk_prefs` and Settings shows an error.

**Acceptance Scenarios**:

1. **Given** EncryptedSharedPreferences initializes successfully, **When** I save a valid key, **Then** it is stored only in the encrypted prefs file.
2. **Given** encryption initialization fails, **When** I attempt to save a key, **Then** save is refused, no plaintext prefs file receives the key, and the UI shows an actionable error.

---

### User Story 3 - Backups exclude secrets and chat data (Priority: P1)

As a user with Auto Backup enabled on my device, I need API key prefs and the CarTalk Room database excluded from backup/restore so chats and credentials do not leave the device via cloud backup (H2, M3).

**Why this priority**: Backup is a common exfiltration path for local data.

**Independent Test**: Inspect `AndroidManifest` + backup/data-extraction XML rules; confirm prefs and DB paths are excluded (or `allowBackup=false`).

**Acceptance Scenarios**:

1. **Given** the installed app, **When** Auto Backup / data extraction rules are evaluated, **Then** `cartalk_secure_prefs`, `cartalk_prefs`, and `cartalk_database` are excluded.
2. **Given** encryption fallback is impossible (US2), **When** backup runs, **Then** no API key material is included.

---

### User Story 4 - Production car hosts are validated (Priority: P2)

As a driver using Android Auto, I need the Car App service to reject untrusted hosts in release builds so a malicious projected host cannot drive the UI (H3).

**Why this priority**: Attack surface is real but requires a hostile Auto host.

**Independent Test**: Release build uses HostValidator allowlist (Play hosts); debug may use ALLOW_ALL only behind `BuildConfig.DEBUG`.

**Acceptance Scenarios**:

1. **Given** a release build, **When** `CarTalkCarAppService.createHostValidator()` runs, **Then** it does not return `ALLOW_ALL_HOSTS_VALIDATOR`.
2. **Given** a debug build, **When** the validator is created, **Then** ALLOW_ALL may be used solely for local development and is documented in `docs/security.md`.

---

### User Story 5 - Key material is not shown in UI (Priority: P2)

As a user entering or reviewing Settings (phone or car), I must not see the full API key prefilled or any key suffix on the car display (M1, M2).

**Why this priority**: Shoulder-surfing / cabin camera risk.

**Independent Test**: Open Settings with a saved key; dialog field empty; status is configured/not configured only; car Setup screen shows no `sk-...xxxx`.

**Acceptance Scenarios**:

1. **Given** a saved API key, **When** I open the Set API Key dialog, **Then** the field is empty (not prefilled with the secret).
2. **Given** a saved API key, **When** I view phone Settings status or car Settings, **Then** only configured/not-configured is shown (no partial key).

---

### User Story 6 - Visual URLs and car delete are hardened (Priority: P3)

As a user with auto-visual enabled, image loads MUST use a fixed Unsplash host and URL-encoded query; as a driver, document delete on car MUST require confirmation (M4, M5).

**Why this priority**: Privacy/injection and accidental destruction — important but secondary to credential leaks.

**Independent Test**: Feed a malicious `searchQuery` with spaces/special chars; assert encoded URL to `images.unsplash.com` or `source.unsplash.com` only. Delete on car shows confirm before remove.

**Acceptance Scenarios**:

1. **Given** visual content with a `searchQuery` containing spaces or reserved characters, **When** the image loads, **Then** the URL is host-allowlisted and the query is URL-encoded.
2. **Given** a document on the car Documents screen, **When** I tap Delete, **Then** I must confirm before the document is removed.

---

### Edge Cases

- What happens when MasterKey creation throws on a locked/broken Keystore device? → Refuse key save; allow app to open Settings with error; chat remains blocked until key can be stored securely.
- What happens when backup rules XML is missing on older APIs? → Manifest `allowBackup` / `dataExtractionRules` / `fullBackupContent` cover API 29+ (minSdk 29).
- What happens when Coil follows redirects off Unsplash? → Prefer disallowing cleartext and keeping fixed HTTPS host; document residual redirect risk if any.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST NOT use `HttpLoggingInterceptor.Level.BODY` in release builds.
- **FR-002**: System MUST redact `x-api-key` (and similar) when HTTP logging is enabled in debug.
- **FR-003**: System MUST fail closed if EncryptedSharedPreferences cannot be created — no plaintext prefs fallback for API keys.
- **FR-004**: System MUST exclude secure prefs, plaintext prefs (if any), and Room DB from Auto Backup / data extraction.
- **FR-005**: Release builds MUST use a non-ALLOW_ALL `HostValidator` for `CarTalkCarAppService`.
- **FR-006**: Settings MUST NOT prefill the full API key into the edit dialog after save.
- **FR-007**: Phone and car UIs MUST NOT display API key suffixes or prefixes.
- **FR-008**: Visual image URLs MUST use URLEncoder and a fixed allowlisted HTTPS host.
- **FR-009**: Car document delete MUST require an explicit confirmation step.
- **FR-010**: `docs/security.md` MUST document mitigations and accepted residual risks (STT, Claude cloud).

### Key Entities

- **ApiKey**: Secret string; stored encrypted at rest; never logged or shown in full after save.
- **BackupExclusionSet**: Named SharedPreferences files and DB file excluded from backup.
- **HostTrustPolicy**: Debug vs release HostValidator selection.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Release Logcat after one successful API call contains zero matches for `sk-ant-` / raw `x-api-key` values.
- **SC-002**: Code path that previously fell back to `cartalk_prefs` for secrets is removed; encryption failure surfaces UI error.
- **SC-003**: Backup/data-extraction XML lists prefs + DB exclusions (or allowBackup disabled).
- **SC-004**: Release `createHostValidator` is not `ALLOW_ALL_HOSTS_VALIDATOR`.
- **SC-005**: Manual Settings/car QA checklist for US5–US6 passes.

## Assumptions

- minSdk 29 remains; data extraction rules are available.
- Upstream PR base remains `claude/android-auto-claude-app-rOVwM`.
- SQLCipher full DB encryption is out of scope; backup exclusion is the control for M3.
- Unsplash remains the visual placeholder provider for this iteration.
