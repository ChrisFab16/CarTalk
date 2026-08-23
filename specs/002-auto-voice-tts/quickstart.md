# Quickstart: Auto Voice & TTS

## Build

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Phone — second mic input (SC-002)

1. Install debug APK; grant microphone.
2. Settings → enable TTS.
3. Chat → tap mic → speak → wait for response.
4. Tap mic again → speak second phrase.
5. **Pass**: both utterances appear as user messages (no busy/error toast).

## Phone — TTS audible (SC-003)

1. TTS enabled; volume up; optionally connect car Bluetooth.
2. Send a message; wait for full assistant reply (not streaming cursor).
3. **Pass**: hear spoken response.

## Car — voice round-trip

1. Open CarTalk in DHU or AA (developer unknown sources).
2. Chat → Speak → utterance → **Pass**: Claude response text updates and TTS plays.

## Real car launcher (documented constraint)

- Sideload only: may **not** appear on OEM launcher — expected per research.md.
- For real car: distribute via Play internal test; re-test launcher visibility.

## Static contract checks

```bash
rg "category.NAVIGATION|startActivity.*RECOGNIZE_SPEECH|override fun onStop" app/src/main/java/com/cartalk/auto
rg "EXTRA_CALLING_PACKAGE|ERROR_RECOGNIZER_BUSY|USAGE_ASSISTANCE" app/src/main/java/com/cartalk/utils
rg "lastSpokenAssistantId|ttsManager.speak" app/src/main/java/com/cartalk/ui/chat
```

Expect: no NAVIGATION category; no RecognizerIntent startActivity in auto/; utils patterns present; chat TTS wiring present.
