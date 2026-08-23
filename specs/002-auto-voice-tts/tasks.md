# Tasks: Auto Voice & TTS Reliability

## Phase 1: Setup

- [x] T001 Create `specs/002-auto-voice-tts/` artifacts; set `.specify/feature.json`

## Phase 2: Android Auto manifest (US1)

- [x] T002 [US1] Remove NAVIGATION category; add `minCarApiLevel` in `AndroidManifest.xml`
- [x] T003 [US1] Document Play distribution constraint in `research.md`

## Phase 3: Speech reliability (US2)

- [x] T004 [US2] Harden `SpeechRecognizerManager.kt` (EXTRA_CALLING_PACKAGE, busy retry, reuse)
- [x] T005 [US2] Wire `ChatCarScreen.kt` to SpeechRecognizerManager (remove RecognizerIntent activity)
- [x] T006 [US2] Wire `TopicCarScreen.kt` topic + question modes to SpeechRecognizerManager

## Phase 4: TTS (US3)

- [x] T007 [US3] Add audio attributes + focus in `TextToSpeechManager.kt`
- [x] T008 [US3] Invoke TTS on completed assistant messages in `ChatFragment.kt`

## Phase 5: Lifecycle (US4)

- [x] T009 [US4] Replace `Screen.onStop` with `onDestroy` observers in car screens
- [x] T010 [US4] Verify `assembleDebug` succeeds

## Phase 6: Validation

- [x] T011 Run static quickstart contract greps
- [ ] T012 Manual sign-off: second mic + audible TTS (record in `validation-results.md`)
- [ ] T013 Sync to `pr/security-hardening` when user requests PR update
