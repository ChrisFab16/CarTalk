# Voice & TTS Contracts

## C-AA-001 — Template app manifest

- CarAppService intent filter: action `androidx.car.app.CarAppService` only (no NAVIGATION category).
- `automotive_app_desc.xml` declares `<uses name="template" />`.
- `androidx.car.app.minCarApiLevel` metadata present.

## C-SPEECH-001 — Recognizer reliability

- Intent includes `RecognizerIntent.EXTRA_CALLING_PACKAGE`.
- On `ERROR_RECOGNIZER_BUSY`: `recognizer.cancel()` then retry `startListening` once if session pending.
- Recognizer created with `applicationContext`.

## C-SPEECH-002 — Car voice wiring

- `ChatCarScreen` / `TopicCarScreen` MUST NOT use `RecognizerIntent` + `startActivity` as primary input path.
- Results MUST reach domain handlers (`sendMessage`, `setTopic`, `handleQuestion`).

## C-TTS-001 — Phone chat playback

- When `PreferencesManager.isTtsEnabled()` and assistant message completes (not streaming), invoke `ttsManager.speak(content)` once per message id.

## C-TTS-002 — Audio routing

- TTS uses `AudioAttributes` with `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` and `CONTENT_TYPE_SPEECH`.
- Request audio focus before `speak()`.

## C-LIFE-001 — Car screen cleanup

- No `override fun onStop` on `Screen` subclasses.
- Cleanup in `DefaultLifecycleObserver.onDestroy`: cancel scope; destroy speech recognizer where owned.
