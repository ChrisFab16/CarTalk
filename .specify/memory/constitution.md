# CarTalk Constitution

## Core Principles

### I. Spec-Driven Changes
All substantial features and security remediations start as Spec Kit artifacts under `specs/<feature>/` (specify → plan → tasks → implement). Do not ship new scope without corresponding specs. Active feature path is recorded in `.specify/feature.json`.

### II. Credentials Fail Closed
Anthropic API keys and other secrets MUST be stored only via EncryptedSharedPreferences (or successor Keystore-backed storage). If encryption initialization fails, the app MUST refuse to save or use the API key and MUST surface a clear Settings error. Silent fallback to plaintext SharedPreferences is forbidden.

### III. Never Log Secrets
Release and debug builds MUST NOT write API keys, `x-api-key` header values, bearer tokens, or full chat request/response bodies containing credentials to Logcat. HTTP logging is gated: BODY level is forbidden in release; headers that carry secrets MUST be redacted when logging is enabled for debug.

### IV. Minimize Exfiltration Surface
Sensitive local data (API key prefs, Room chat/document DB) MUST be excluded from Android Auto Backup / data extraction unless explicitly justified. Third-party URLs built from model output MUST use fixed hosts, URL encoding, and no open redirect trust.

### V. Android Auto Host Trust
Production builds MUST NOT use `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR`. Host allowlisting follows Car App Library guidance (Play / OEM hosts). Debug-only relaxations require `BuildConfig.DEBUG` gates and documentation in `docs/security.md`.

### VI. Simplicity & Driver Safety
Prefer minimal diffs. Car UI must avoid displaying secret material (including key suffixes). Destructive actions from the car (e.g. document delete) require confirmation. Keep voice-friendly responses; do not expand scope into unrelated product features during security work.

## Security Requirements

- Transport to Anthropic is HTTPS only with the OS trust store (certificate pinning optional, not required for MVP).
- API key UI: password input; do not prefill the full key after save; status may show configured/not configured only (no partial key on car display).
- Living posture is maintained in `docs/security.md` (mitigations + accepted gaps such as cloud STT / Claude processing).

## Development Workflow

1. Work lands on fork branch `main` with Spec Kit artifacts.
2. Publish upstream-ready changes via `pr/security-hardening` (code + feature specs + `docs/security.md`; exclude agent-only install noise when it bloats the PR).
3. Before shipping auth, credentials, network, backup, or deletion changes: run adversarial security review (`/security-review` skill).

## Governance

This constitution supersedes informal practices for CarTalk. Amendments update this file and bump the version below. PRs that touch credentials, logging, backup, or Car App host validation MUST cite the relevant principle.

**Version**: 1.0.0 | **Ratified**: 2026-08-21 | **Last Amended**: 2026-08-21
