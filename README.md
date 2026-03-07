# CarTalk

A Claude-powered Android Auto companion app for in-car conversations, learning, and idea capture.

## Features

### 💬 Chat with Claude
- Voice-first conversation using Android's speech recognition
- Streaming responses for natural conversation flow
- Text-to-speech playback of responses

### 📝 Document Creation While Chatting
- **Save Note** — Save any exchange as a note while chatting
- **Capture Idea** — Quickly capture an idea that came up in conversation
- All documents stored locally with Room database

### 📚 Deep Dive Mode
- Start an in-depth learning session on any topic
- Claude provides comprehensive, educational responses
- Uses extended thinking (adaptive) for deeper reasoning

### 📋 Recap Documents
- Auto-generate a structured recap after any Deep Dive session
- Includes key points, insights, and action items
- Append additional notes to existing documents

### ⚙️ Setup & Credentials
- Secure API key storage using Android's EncryptedSharedPreferences
- Model selection (Opus 4.6, Sonnet 4.6, Haiku 4.5)
- Toggle TTS, deep thinking, and auto-visual detection

### 🖼️ Visual Display
- Auto-detects landmarks, people, and visual content in conversations
- Shows relevant images on screen (phone) or descriptions (car display)
- Pops up visual panel without interrupting chat flow

## Android Auto
Full Android Auto support via Car App Library:
- Main menu navigation
- Voice-driven chat
- Topic deep dive with suggested topics
- Documents browser with TTS read-aloud
- Settings screen (directs to phone for API key entry)

## Setup

1. Get a Claude API key at [console.anthropic.com](https://console.anthropic.com)
2. Open CarTalk on your phone
3. Go to **Settings** → **Set API Key**
4. Enter your `sk-ant-...` API key
5. Start chatting!

## Architecture

- **Kotlin** + Android Car App Library
- **Claude API** via OkHttp (streaming + standard requests)
- **Room** for local document and message storage
- **MVVM** with StateFlow for reactive UI
- **EncryptedSharedPreferences** for secure API key storage
- **Android TTS + SpeechRecognizer** for voice I/O
