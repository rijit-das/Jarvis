# JARVIS Mobile — Product Requirements Document

**Version:** 1.0 (initial implementation)
**Platform:** Android (API 26+ / Android 8.0+), Kotlin, Jetpack Compose
**Positioning:** Privacy-first, offline-first personal AI assistant.

## 1. Problem

Mainstream voice assistants route audio and personal data to the cloud by default.
JARVIS Mobile inverts that default: everything that *can* run on-device *does* run
on-device, and anything that needs the network is opt-in, visible, and revocable.

## 2. Goals

1. Natural spoken and typed interaction with honest capability boundaries.
2. Fully functional offline core: intents, tools, memory, reminders, notes.
3. User-controlled memory: inspect, edit, export, delete.
4. Permission-based device actions with confirmation for consequential operations.
5. Battery- and memory-conscious behaviour on low-end devices.
6. Transparent security posture: audit log, offline/online indicator, no hidden
   recording or data collection.

## 3. Non-goals (v1)

- Bundled on-device large language model (see `docs/KNOWN_LIMITATIONS.md` and
  `docs/ROADMAP.md` for the integration path).
- Continuous always-on wake-word operation on all devices (best-effort, opt-in).
- Messaging, payments, or any irreversible transaction without explicit
  confirmation UI.
- Accessibility Service automation.

## 4. Functional requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Text chat with context-aware offline intent engine | Must |
| FR-2 | Voice input (SpeechRecognizer, offline-preferred) with visible state | Must |
| FR-3 | Voice output (TTS), adjustable rate, interruptible | Must |
| FR-4 | Wake-word activation ("Hey JARVIS"), opt-in, foreground-service bound | Must |
| FR-5 | Clap activation (double-clap validation), opt-in, configurable | Must |
| FR-6 | Tools: time/date, timers, reminders, notes, tasks, open-app, media, notifications (opt-in), navigation handoff | Must |
| FR-7 | Local memory: preferences, facts, notes, tasks, reminders, session context | Must |
| FR-8 | Memory management UI: inspect/edit/export/delete | Must |
| FR-9 | Optional online AI provider (OpenAI-compatible), API key in Keystore-backed storage, consent-gated | Should |
| FR-10 | Offline mode enforcement (network blocked at app layer) | Must |
| FR-11 | Activity/audit history | Must |
| FR-12 | Permissions dashboard with rationale and status | Must |
| FR-13 | Diagnostics screen (device, mic, STT/TTS, storage, memory) | Must |
| FR-14 | Model manager with device-compatibility tiers and graceful fallback | Must |

## 5. Non-functional requirements

- **Privacy:** no audio upload without explicit consent; no raw mic recordings
  persisted; no secrets in source code; encrypted local storage.
- **Performance:** no UI-thread blocking; expensive work on background dispatchers;
  mic released when idle; low-memory graceful degradation.
- **Reliability:** recover from process death; handle mic interruptions; verify
  tool results and report failures honestly.
- **Security:** least-privilege permissions; tool allowlist; confirmation for
  consequential actions; prompt-injection guard for external content.

## 6. Success criteria (v1)

- Builds cleanly in Android Studio; unit tests pass in CI.
- All FR-1…FR-14 features demonstrably work on a physical device or emulator.
- No placeholder buttons: every visible control performs a real action or is
  removed.
- Offline mode: airplane-mode test passes for all offline features.
