# CarTalk — Agent guidance

## Spec Kit (required for substantial work)

Follow Spec Kit for all features and security remediations:

1. `/speckit-specify` → `specs/<NNN-feature>/spec.md`
2. `/speckit-plan` → `plan.md` (+ research / contracts / quickstart as needed)
3. `/speckit-tasks` → `tasks.md`
4. `/speckit-implement` (or implement against tasks)

Active feature directory: `.specify/feature.json`.

Do not implement new scope without corresponding artifacts under `specs/`.

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
