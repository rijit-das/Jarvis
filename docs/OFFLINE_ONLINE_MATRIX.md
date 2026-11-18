# JARVIS Mobile — Offline / Online Feature Matrix

**Offline mode is the default.** Online mode is opt-in, consent-gated, and
always indicated in the UI.

| Feature | Offline | Online (opt-in) | Notes |
|---------|---------|-----------------|-------|
| Text chat (intent engine) | ✅ | ✅ | Deterministic, on-device |
| Voice input (STT) | ✅* | ✅ | *Requires an installed recognizer with offline pack; otherwise falls back to text |
| Voice output (TTS) | ✅ | ✅ | On-device engine |
| Wake word | ✅* | ✅ | *Best-effort; recognizer-dependent (see KNOWN_LIMITATIONS) |
| Clap detection | ✅ | ✅ | Fully on-device PCM analysis |
| Time/date, timers, reminders | ✅ | ✅ | AlarmManager + WorkManager |
| Notes, tasks, memory | ✅ | ✅ | Encrypted local DB |
| Open app, media control, navigation handoff | ✅ | ✅ | Android intents / MediaSession |
| Notification reading | ✅ | ✅ | Requires user-enabled Notification Listener |
| Open-ended conversation (LLM) | ❌ | ✅ | Online provider only in v1; on-device LLM is roadmap |
| Model downloads | ❌ | ✅ | Only for user-initiated model fetch (roadmap) |

## Offline-mode enforcement

When offline mode is ON (default):
- `OnlineProvider` refuses to execute; the orchestrator never routes to it.
- The connectivity monitor still reports state, but no network calls are made.
- UI shows a persistent "Offline" status chip.

When online mode is ON:
- User must have (a) enabled online mode in Settings, (b) acknowledged the
  data-sharing notice, (c) provided an API key (stored encrypted).
- Every online request is audit-logged (metadata only, never content secrets).
- Status chip shows "Online" whenever a request is in flight.
