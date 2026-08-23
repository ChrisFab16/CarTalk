# Validation Results — 002 Auto Voice & TTS

**Date**: 2026-08-23

## Automated / static

| Check | Result | Notes |
|-------|--------|-------|
| `./gradlew assembleDebug` | PASS | Build successful after lifecycle + import fixes |
| C-AA-001 manifest | PASS | No NAVIGATION category; minCarApiLevel=1 |
| C-SPEECH-001 recognizer | PASS | EXTRA_CALLING_PACKAGE + busy retry in code |
| C-SPEECH-002 car wiring | PASS | ChatCarScreen/TopicCarScreen use SpeechRecognizerManager |
| C-TTS-001 phone chat | PASS | lastSpokenAssistantId + ttsManager.speak in ChatFragment |
| C-TTS-002 audio routing | PASS | USAGE_ASSISTANCE_NAVIGATION_GUIDANCE + focus request |

## Manual (operator sign-off required)

| Scenario | Status | Operator |
|----------|--------|----------|
| SC-002 Two consecutive phone mic inputs | PENDING | User |
| SC-003 Audible TTS on phone chat | PENDING | User |
| Car Speak round-trip (DHU/AA) | PENDING | User |
| Real OEM launcher with Play build | PENDING | Requires Play internal test track |
