# Quickstart: Security Hardening verification

## Prerequisites

- Android device or emulator with ADB
- Anthropic API key for live call (optional for static checks)

## Static checks (always)

1. Open `ClaudeApiService.kt` — confirm release path is not `Level.BODY`.
2. Open `PreferencesManager.kt` — confirm no plaintext SharedPreferences fallback for secrets.
3. Open `AndroidManifest.xml` — confirm backup/dataExtraction rules and XML exclusions.
4. Open `CarTalkCarAppService.kt` — confirm release validator ≠ ALLOW_ALL.
5. Open Settings / SetupCarScreen — no key suffix display; dialog not prefilled.
6. Open `ChatFragment` — Unsplash URL uses `URLEncoder`.
7. Open car document delete — confirmation before delete.
8. Read `docs/security.md` — mitigations + accepted gaps listed.

## Runtime (recommended)

```bash
adb logcat -c
# Launch release or debug build, configure key if needed, send one chat
adb logcat -d | rg -i 'sk-ant-|x-api-key'
```

Expect: no matches for real key material (redacted `***` in debug headers is OK).

## Backup (optional)

Use `adb backup` / device backup settings only in a test profile; restore must not reinstate API key from excluded prefs (encrypted or plaintext).
