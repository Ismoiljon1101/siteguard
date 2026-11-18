# Change request: operator credentials (schema v4)

Status: implemented, awaiting TL acceptance. Affects Part 2 of the build brief ("`OPERATOR` has no credentials in this version") and D17 (pick a name, no password).

## Change

Migration 4 adds credential columns to `operators`:

| Column | Type | Meaning |
|---|---|---|
| `password_hash` | TEXT | Base64 of the 256-bit PBKDF2 key |
| `salt` | TEXT | Base64 of 16 random bytes, per operator |
| `iterations` | INTEGER | Iteration count used for this row, so it can be raised later |
| `created_at` | TEXT | When the credential was set (UTC) |
| `failed_attempts` | INTEGER NOT NULL DEFAULT 0 | Consecutive failures |
| `locked_until` | TEXT | UTC time the lock ends, NULL when not locked |

Migration 3 also adds `operators.login_id TEXT UNIQUE` (`director`, `manager`, `supervisor`). The brief's `operator_id` stays an integer primary key because `incident_actions.operator_id` and `helmet_assignments.assigned_by` reference it.

## Algorithm

PBKDF2WithHmacSHA256 from the JDK, 256-bit key, random 16-byte salt, constant-time compare (`MessageDigest.isEqual`). The code uses 600,000 iterations.

Source: OWASP Password Storage Cheat Sheet, "PBKDF2-HMAC-SHA256: 600,000 iterations (recommended)". https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html (fetched 2026-11-18). The same URL is in the `PasswordHasher` class comment.

## Throttle

Five failed attempts lock the operator for 60 s (injected `Clock`). While locked, even the correct password is refused and does not count. A success clears the counter. Unknown operator, wrong password and locked account all give the same message and run a dummy hash, so the response does not reveal which case it was. Caveat: throttling is per existing operator. Attempts against unknown IDs are not counted.

## First-run seed

Three operators, IDs `director`, `manager`, `supervisor` (roles SAFETY_DIRECTOR, SITE_MANAGER, SUPERVISOR). Passwords are 16 random characters (`SecureRandom`, no `0 O 1 l I`). The plaintext is printed to the console once and written only to `data/demo-credentials.txt`. `data/` is git-ignored (`git check-ignore -v data/demo-credentials.txt` matches `.gitignore:32:data/`). Only hashes reach the database. The seed does nothing when any operator exists. A lost password means deleting `data/siteguard.db` (demo data only) or adding a reset feature, which is not built.

## Decision needed

D17 said "no password in the single-process demo". This implements login anyway because the task asked for it. TL: accept, or tell the UI to keep the name picker and ignore the credential columns (they are harmless).
