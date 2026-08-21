# Implementation Plan: Security Hardening

**Branch**: `main` → publish `pr/security-hardening` | **Date**: 2026-08-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-security-hardening/spec.md`

## Summary

Close credential leak and backup/host trust gaps identified in the adversarial review by: gating OkHttp logging, fail-closed encrypted prefs, backup exclusion XML, release HostValidator allowlist, scrubbing key UI, hardening Unsplash URL construction, and confirming car deletes. Document posture in `docs/security.md`.

## Technical Context

**Language/Version**: Kotlin 1.9 / JVM 17

**Primary Dependencies**: OkHttp 4.12, androidx.security:security-crypto, Android Car App Library 1.4, Room 2.6, Coil 2.7

**Storage**: EncryptedSharedPreferences (API key); Room SQLite (chats/docs, plaintext — excluded from backup)

**Testing**: Manual Logcat/backup inspection per quickstart; no new unit test framework required for this pass

**Target Platform**: Android phone + Android Auto (minSdk 29, targetSdk 34)

**Project Type**: mobile-app (single Gradle module `app/`)

**Performance Goals**: N/A (security hardening)

**Constraints**: Fail closed on crypto; no BODY logging in release; no secret UI on car

**Scale/Scope**: ~10 files touched; findings C1, H1–H3, M1–M5

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status |
|-----------|--------|
| I. Spec-Driven Changes | Pass — this feature has spec/plan/tasks |
| II. Credentials Fail Closed | Pass — design removes plaintext fallback |
| III. Never Log Secrets | Pass — release NONE + redact debug |
| IV. Minimize Exfiltration Surface | Pass — backup exclusions + URL allowlist |
| V. Android Auto Host Trust | Pass — release non-ALLOW_ALL |
| VI. Simplicity & Driver Safety | Pass — confirmation on car delete; no key on car UI |

Post-design: unchanged — all gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/001-security-hardening/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── security-contracts.md
└── tasks.md
```

### Source Code (repository root)

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/cartalk/
│   ├── api/ClaudeApiService.kt
│   ├── auto/CarTalkCarAppService.kt
│   ├── auto/SetupCarScreen.kt
│   ├── auto/DocumentsCarScreen.kt
│   ├── ui/settings/SettingsFragment.kt
│   ├── ui/chat/ChatFragment.kt
│   └── utils/PreferencesManager.kt
└── res/xml/
    ├── backup_rules.xml
    └── data_extraction_rules.xml
docs/security.md
```

## Complexity Tracking

No constitution violations requiring justification.
