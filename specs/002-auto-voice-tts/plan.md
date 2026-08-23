# Implementation Plan: Auto Voice & TTS Reliability

**Branch**: `main` | **Date**: 2026-08-23 | **Spec**: [spec.md](./spec.md)

## Summary

Fix Android Auto template declaration, robust speech recognition for repeat inputs, wire phone chat TTS, route TTS audio for car Bluetooth, replace broken car voice intents with in-process recognizer, and fix Car App lifecycle compile errors.

## Technical Context

**Language/Version**: Kotlin 1.9 / Android minSdk 29  
**Primary Dependencies**: Android Car App Library 1.4, SpeechRecognizer, TextToSpeech  
**Testing**: Manual quickstart + `assembleDebug`  
**Target Platform**: Phone + Android Auto projection  

## Constitution Check

| Principle | Status |
|-----------|--------|
| I. Spec-Driven Changes | Retroactive spec (this feature) before commit |
| VI. Simplicity & Driver Safety | Voice/TTS only; no unrelated scope |

## Project Structure

```text
specs/002-auto-voice-tts/
├── spec.md, plan.md, research.md, tasks.md
├── contracts/voice-tts-contracts.md
├── quickstart.md
└── validation-results.md

app/src/main/
├── AndroidManifest.xml
├── java/com/cartalk/utils/SpeechRecognizerManager.kt
├── java/com/cartalk/utils/TextToSpeechManager.kt
├── java/com/cartalk/ui/chat/ChatFragment.kt
└── java/com/cartalk/auto/{ChatCarScreen,TopicCarScreen,DocumentsCarScreen,VisualCarScreen}.kt
```
