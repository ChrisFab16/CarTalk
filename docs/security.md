# CarTalk security posture

**Last updated**: 2026-08-21

## Threat model (summary)

- Device with ADB / debug access
- Android Auto Backup / data extraction
- Untrusted or malicious Android Auto projected host
- Shoulder-surfing / cabin visibility of Settings and car UI
- LLM-derived strings used in third-party image URLs

## Mitigations

| Area | Control |
|------|---------|
| API key at rest | EncryptedSharedPreferences + MasterKey; **fail closed** if crypto init fails |
| API key in transit logs | Release: no OkHttp BODY/HEADERS dump of secrets; debug: redact `x-api-key` |
| Backup | `fullBackupContent` + `dataExtractionRules` exclude secure prefs, legacy prefs, Room DB |
| Car App hosts | Release: allowlist HostValidator; Debug: ALLOW_ALL for local development only |
| Key UI | No full-key prefill; no key suffixes on phone status or car Setup |
| Visual images | Fixed Unsplash HTTPS host + URL-encoded query |
| Car delete | Confirmation step before document deletion |

## Accepted residual risks

- **Cloud speech recognition**: Audio/transcripts may leave the device via the platform STT engine.
- **Claude API**: Conversation content is sent to Anthropic over HTTPS by design.
- **Room DB plaintext**: Chat/documents are not SQLCipher-encrypted; exclusion from backup is the control in this release.
- **No certificate pinning**: Relies on OS PKI trust for `api.anthropic.com`.
- **Coil redirects**: Image loads use a fixed Unsplash host; residual risk if the CDN issues unexpected redirects is accepted for placeholder visuals.

## Review history

- 2026-08-21: Adversarial review (C1, H1–H3, M1–M5) → feature `specs/001-security-hardening`.
