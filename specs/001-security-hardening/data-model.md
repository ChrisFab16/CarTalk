# Data Model: Security Hardening

No new Room entities. Changes are to storage policy and UI presentation of existing concepts.

## ApiKey (logical)

| Field | Notes |
|-------|--------|
| value | Secret string (`sk-ant-…`); never logged; never shown in full after save |
| storage | EncryptedSharedPreferences file `cartalk_secure_prefs` only |
| availability | `secureStorageAvailable: Boolean` — false ⇒ refuse set/get for network use |

## BackupExclusionSet

| Resource | Type | Exclusion |
|----------|------|-----------|
| `cartalk_secure_prefs` | sharedpref | exclude |
| `cartalk_prefs` | sharedpref | exclude (legacy/plaintext; wipe key if found) |
| `cartalk_database` | database | exclude |

## HostTrustPolicy

| Build | Validator |
|-------|-----------|
| `DEBUG` | `ALLOW_ALL_HOSTS_VALIDATOR` (documented) |
| release | Allowlist validator (Play/OEM hosts) |

## VisualImageRequest

| Field | Constraint |
|-------|------------|
| host | Fixed `source.unsplash.com` (HTTPS) |
| query | URL-encoded `searchQuery` from model |
