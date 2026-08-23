# Feature Specification: Auto Voice & TTS Reliability

**Feature Branch**: `main`

**Created**: 2026-08-23

**Status**: Implemented (retroactive spec for session fixes)

**Input**: User reports: CarTalk not selectable in Android Auto launcher; speech transcription fails on second input; TTS does not speak aloud (phone + car).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Android Auto discovers CarTalk (Priority: P1)

As a driver, I want CarTalk to appear as a template app in Android Auto when properly distributed, so I can launch it from the car.

**Why this priority**: Without launcher visibility, car UI is unreachable.

**Independent Test**: DHU or phone AA launcher shows CarTalk after developer unknown-sources enabled; document Play-distribution requirement for real head units.

**Acceptance Scenarios**:

1. **Given** sideloaded debug APK and AA developer settings, **When** user opens AA launcher customization, **Then** CarTalk may appear (DHU/phone projection).
2. **Given** real OEM head unit, **When** app is sideloaded only, **Then** spec documents that Play internal testing is required (accepted platform constraint).

---

### User Story 2 - Repeated voice input works (Priority: P1)

As a user, I want to tap the mic multiple times and get transcription each time, on phone and in car chat.

**Why this priority**: Core voice-first UX was broken after first utterance.

**Independent Test**: Phone Chat — mic → speak → send → mic again → second utterance transcribed without error toast.

**Acceptance Scenarios**:

1. **Given** RECORD_AUDIO granted, **When** user completes two consecutive mic sessions on phone, **Then** both produce text (no `ERROR_RECOGNIZER_BUSY` loop).
2. **Given** car Chat/Topic screens, **When** user taps Speak twice across turns, **Then** `SpeechRecognizerManager` delivers results to `sendMessage` / `handleQuestion` (not external intent with no callback).

---

### User Story 3 - TTS speaks responses aloud (Priority: P1)

As a user with TTS enabled, I want to hear Claude's replies on phone chat and car screens, routed to car audio when connected.

**Why this priority**: TTS existed but was unwired or inaudible.

**Independent Test**: Settings TTS on → send chat on phone → hear speech; car chat → hear response after API reply.

**Acceptance Scenarios**:

1. **Given** TTS enabled in Settings, **When** assistant message completes on phone Chat, **Then** `TextToSpeechManager.speak` is invoked once per new assistant message.
2. **Given** Bluetooth/car audio connected, **When** TTS speaks, **Then** audio uses `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` and requests audio focus.

---

### User Story 4 - Car App lifecycle compiles and cleans up (Priority: P2)

As a maintainer, I need Car App screens to use supported lifecycle APIs so the project builds and resources release on destroy.

**Acceptance Scenarios**:

1. **Given** Car App Library 1.4, **When** project compiles, **Then** no `Screen.onStop()` overrides (use `DefaultLifecycleObserver.onDestroy`).
2. **Given** user leaves a car screen, **When** screen is destroyed, **Then** coroutines and speech recognizer are cancelled/destroyed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: CarAppService intent filter MUST NOT declare `NAVIGATION` category (template app only).
- **FR-002**: Manifest MUST include `androidx.car.app.minCarApiLevel` metadata.
- **FR-003**: `SpeechRecognizerManager` MUST set `EXTRA_CALLING_PACKAGE` and handle `ERROR_RECOGNIZER_BUSY` with cancel+retry.
- **FR-004**: Car voice MUST use in-process `SpeechRecognizerManager`, not `RecognizerIntent` via `startActivity` without result handling.
- **FR-005**: Phone `ChatFragment` MUST invoke TTS when a new assistant message completes and TTS is enabled.
- **FR-006**: `TextToSpeechManager` MUST set speech `AudioAttributes` and request audio focus before speak.
- **FR-007**: Car screens MUST use `lifecycle.addObserver` + `onDestroy` for cleanup, not override `Screen.onStop`.

### Key Entities

- **SpeechSession**: Recognizer state; busy/retry semantics.
- **TtsPlayback**: Enabled flag + audio routing + utterance queue.

## Success Criteria *(mandatory)*

- **SC-001**: `./gradlew assembleDebug` succeeds.
- **SC-002**: Manual quickstart: two consecutive phone mic inputs succeed.
- **SC-003**: Manual quickstart: audible TTS on phone chat with setting enabled.
- **SC-004**: `research.md` documents real-car Play distribution constraint.

## Assumptions

- User tests on phone projection and/or DHU; real car may require Play distribution.
- Gradle wrapper added to repo for reproducible builds.
