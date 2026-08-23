# Research: Auto Voice & TTS

## Decision 1: Android Auto launcher visibility

**Decision**: Remove `NAVIGATION` category; keep `template` in `automotive_app_desc.xml`; add `minCarApiLevel`.

**Rationale**: CarTalk is not a navigation app; wrong category can affect host filtering.

**Platform constraint (real car)**: Sideloaded APKs often do **not** appear on OEM head units. Play Internal Testing or Internal App Sharing is required for real-vehicle validation ([Android Auto testing docs](https://developer.android.com/training/cars/testing#real-vehicles)).

## Decision 2: SpeechRecognizer restart

**Decision**: Reuse recognizer instance; `EXTRA_CALLING_PACKAGE`; on `ERROR_RECOGNIZER_BUSY` call `cancel()` and retry after 250ms.

**Rationale**: Destroy/recreate each tap causes second-input failures (ERROR_RECOGNIZER_BUSY / ERROR_CLIENT).

## Decision 3: Car voice input

**Decision**: Replace `carContext.startActivity(RecognizerIntent)` with shared `SpeechRecognizerManager` callbacks into `sendMessage` / `setTopic` / `handleQuestion`.

**Rationale**: Activity-based recognizer does not return results to Car App `Screen` without a result relay; previous implementation was non-functional.

## Decision 4: TTS routing

**Decision**: `AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` + `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`; wire phone `ChatFragment` to speak on completed assistant messages when prefs allow.

**Rationale**: Manager existed but phone chat never called `speak()`; default TTS stream may not route to car Bluetooth.

## Decision 5: Car App lifecycle

**Decision**: Use `DefaultLifecycleObserver.onDestroy` instead of overriding `Screen.onStop()` (not available in Car App Library 1.4).

**Rationale**: Build failed with `'onStop' overrides nothing`.
