# CarTalk — Agent guidance

## Spec Kit (required for substantial work)

Follow Spec Kit for all features and security remediations:

1. `/speckit-specify` → `specs/<NNN-feature>/spec.md`
2. `/speckit-plan` → `plan.md` (+ research / contracts / quickstart as needed)
3. `/speckit-tasks` → `tasks.md`
4. **`/speckit-analyze` (mandatory gate)** — run immediately after tasks, **before any implementation**. Write `specs/<feature>/analyze-report.md`. Do **not** start `/speckit-implement` or feature code until analyze completes and all **CRITICAL** findings are resolved (or explicitly accepted by the user in spec/tasks). Resolve **HIGH** findings in artifacts or tasks before marking the feature done.
5. `/speckit-implement` (or implement against tasks)
6. Record manual/live checks in `specs/<feature>/validation-results.md`

Active feature directory: `.specify/feature.json`.

Do not implement new scope without corresponding artifacts under `specs/`.

**Scope extensions on an active feature** (e.g. pre-download added mid-003): update `spec.md` + `plan.md` (+ contracts if needed) → append tasks → **`/speckit-analyze`** → implement. Do not use `/speckit-converge` alone for new user-requested capability; converge is for post-implement gaps, not skipping the analyze gate. (2026-08-23)

**Do not mark build/validation tasks `[x]`** until `./gradlew assembleDebug` (or the task’s stated command) passes on the current tree — including after new native/Maven dependencies. (2026-08-23)

**Voice / STT / mic lifecycle changes**: `/speckit-analyze` is artifact consistency only; before marking implement phases done, run `/universal-code-review` (or equivalent) for mic stop/teardown, download threading, and session-state paths — or keep manual quickstart sign-off explicitly open. (2026-08-23)

**User-reported bugs and runtime fixes** (voice, TTS, Android Auto, permissions) are new scope — open a new feature spec or `/speckit-converge` before coding; do not patch ad hoc on `main`. (2026-08-23)

**After a spec'd feature ships**, if the user reports unfixed behavior, re-run converge/analyze against that feature before claiming "already done". (2026-08-23)

**Platform-gated features** (Android Auto on real OEM head units): `research.md` MUST document distribution constraints (e.g. Play internal test required; sideload may not show in launcher). (2026-08-23)

**Voice / TTS / mic flows**: quickstart MUST include at least one repeat-input case and audible-output verification — not only "manager class exists". (2026-08-23)

Constitution: `.specify/memory/constitution.md`.

## Branch workflow

| Branch | Purpose |
|--------|---------|
| `main` | Spec Kit home + implementation (daily work) |
| `pr/security-hardening` | Clean PR head for upstream (`ommereer/CarTalk`) |
| `claude/android-auto-claude-app-rOVwM` | Upstream default / sync mirror |

Implement on `main`. When ready, sync intentional paths (app code, `specs/…`, `docs/security.md`) onto `pr/security-hardening` and open the PR.

## Security posture

See `docs/security.md`. Hard rules from the constitution:

- Credentials fail closed (no plaintext prefs fallback)
- Never log API keys or BODY-level OkHttp traffic in release
- Exclude secrets and chat DB from Auto Backup
- Production Car App host validator must not be ALLOW_ALL
- No API key material on Android Auto UI

## Remotes

- `origin` → `ChrisFab16/CarTalk` (fork)
- `upstream` → `ommereer/CarTalk`
