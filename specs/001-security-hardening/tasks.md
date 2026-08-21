# Tasks: Security Hardening

**Input**: Design documents from `/specs/001-security-hardening/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Manual per quickstart.md (no new automated test framework in this pass)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to

## Phase 1: Setup

- [x] T001 Create `specs/001-security-hardening/` artifacts and `.specify/feature.json`
- [x] T002 [P] Add `docs/security.md` posture document (mitigations + accepted gaps)

## Phase 2: Foundational

- [x] T003 Enable `buildConfig = true` in `app/build.gradle.kts` if needed for `BuildConfig.DEBUG` host gating

## Phase 3: User Story 1 - API traffic does not leak credentials (P1)

**Goal**: Stop Logcat credential/body leaks

- [x] T004 [US1] Gate OkHttp `HttpLoggingInterceptor` in `app/src/main/java/com/cartalk/api/ClaudeApiService.kt` — NONE in release; redacted headers in debug
- [x] T005 [US1] Verify no `Level.BODY` remains for release paths (static review)

## Phase 4: User Story 2 - API key storage fails closed (P1)

**Goal**: Remove plaintext prefs fallback

- [x] T006 [US2] Refactor `app/src/main/java/com/cartalk/utils/PreferencesManager.kt` to fail closed; expose secure-storage availability; wipe legacy plaintext key if present
- [x] T007 [US2] Update `app/src/main/java/com/cartalk/ui/settings/SettingsFragment.kt` to show error when secure storage unavailable or save fails

## Phase 5: User Story 3 - Backups exclude secrets and chat data (P1)

**Goal**: Exclude prefs + Room DB from backup

- [x] T008 [P] [US3] Add `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml` excluding `cartalk_secure_prefs`, `cartalk_prefs`, `cartalk_database`
- [x] T009 [US3] Wire rules into `app/src/main/AndroidManifest.xml` (`fullBackupContent`, `dataExtractionRules`)

## Phase 6: User Story 4 - Production car hosts are validated (P2)

**Goal**: Release HostValidator allowlist

- [x] T010 [US4] Update `app/src/main/java/com/cartalk/auto/CarTalkCarAppService.kt` — ALLOW_ALL only if debuggable; else allowlist validator

## Phase 7: User Story 5 - Key material is not shown in UI (P2)

**Goal**: No key prefill or suffixes

- [x] T011 [P] [US5] Update `SettingsFragment.kt` — empty dialog field; status configured/not configured only
- [x] T012 [P] [US5] Update `SetupCarScreen.kt` — no key suffix on car UI

## Phase 8: User Story 6 - Visual URLs and car delete (P3)

**Goal**: Safe Unsplash URL + delete confirm

- [x] T013 [P] [US6] Harden Unsplash URL in `app/src/main/java/com/cartalk/ui/chat/ChatFragment.kt` with `URLEncoder` + fixed host
- [x] T014 [US6] Add confirmation before delete in `app/src/main/java/com/cartalk/auto/DocumentsCarScreen.kt`

## Phase 9: Polish

- [x] T015 Run quickstart.md static checklist and mark contracts satisfied
- [x] T016 Commit on `main`; sync intentional paths to `pr/security-hardening`; open upstream PR

## Dependency graph

```text
T001 → T002
T003 → T010
T004 → T005
T006 → T007
T008 → T009
T011, T012 independent after T006
T013, T014 independent
T015 after all US tasks
T016 last
```

## Parallel opportunities

- T002 || T003
- T008 || T004 || T006
- T011 || T012 || T013 || T014 after their prerequisites
