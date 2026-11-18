# JARVIS Mobile — Threat Model

## 1. Assets

- Voice input and transcripts (potentially sensitive speech).
- Memories, notes, tasks, reminders (personal data).
- API keys for the optional online provider.
- Device capabilities (apps, media, notifications).

## 2. Trust boundaries

1. **User ↔ JARVIS** — trusted, but JARVIS still confirms consequential actions.
2. **JARVIS ↔ Android platform** — permissions are the enforcement boundary;
   JARVIS never bypasses them.
3. **JARVIS ↔ Network (online mode)** — untrusted; only active with explicit
   consent; TLS only; no private data sent without user approval.
4. **External content → model** — untrusted (prompt injection surface);
   sanitised by `PromptInjectionGuard` before reaching any provider.
5. **JARVIS ↔ Local storage** — encrypted at rest (SQLCipher + Keystore).

## 3. Threats & mitigations

| # | Threat | Mitigation |
|---|--------|------------|
| T1 | Mic used without user knowledge | Visible listening indicator, stop control, FGS notification, Android mic privacy indicators respected; mic released when idle |
| T2 | Audio uploaded without consent | Offline-first STT (`EXTRA_PREFER_OFFLINE`); online mode is opt-in and indicated; no raw recordings persisted |
| T3 | API key leakage | Keystore-backed EncryptedSharedPreferences; never logged; never in source |
| T4 | Prompt injection via external content (e.g., notification text, web content) | `PromptInjectionGuard` strips instruction-like payloads; tool allowlist; model cannot invoke tools outside registry |
| T5 | Model overrides security policy via conversation | Security policy is code, not prompt text; confirmation gates are enforced by `ToolExecutor`, not by the model |
| T6 | Data theft from lost device | SQLCipher-encrypted DB; Keystore-held passphrase; no cloud backup of sensitive data (`allowBackup=false` for DB) |
| T7 | Over-privileged permissions | Least-privilege matrix (`PERMISSION_MATRIX.md`); in-context requests only |
| T8 | Silent memory creation | Memory writes require explicit user action or opt-in category; all writes auditable |
| T9 | Malicious tool invocation | Allowlist + input validation + timeouts + audit log on every execution |
| T10 | Network exfiltration in offline mode | `ConnectivityMonitor` + offline-mode gate blocks provider calls; status always visible |

## 4. Out of scope / residual risks

- A compromised OS or rooted device defeats local encryption guarantees.
- SpeechRecognizer/TTS engines provided by third-party apps (e.g., Google app)
  are outside JARVIS's control; the privacy implications are disclosed in the
  Privacy screen.
- Online provider responses are only as trustworthy as the provider; JARVIS
  labels online-generated content.

## 5. Security invariants (enforced in code, covered by tests)

1. No network call without online mode + consent + connectivity.
2. No tool execution without allowlist + permission + (confirmation if required).
3. No secret ever written to logs or audit entries.
4. No microphone capture outside `VoiceSessionManager`.
5. No permanent memory write without explicit user intent.
