# JARVIS Mobile — Implementation Roadmap

## Status legend

- ✅ Implemented in this codebase
- 🚧 Implemented with documented limitations
- 🔜 Roadmap (not implemented; do not claim otherwise)

## Phase 1 — Architecture ✅

PRD, architecture, permission matrix, threat model, compatibility, this roadmap.

## Phase 2 — Foundation ✅

Android project (Kotlin/Compose/Material 3), navigation, settings, permission
handling, secure storage (Keystore + EncryptedSharedPreferences + SQLCipher).

## Phase 3 — Voice ✅ / 🚧

- Speech input (SpeechRecognizer, offline-preferred) ✅
- Speech output (TTS, rate/voice config, interruptible) ✅
- Wake-word module (recognizer-based, foreground session, opt-in) 🚧
- Clap detection (validated double-clap, configurable) ✅
- Microphone lifecycle (`VoiceSessionManager`) ✅

## Phase 4 — AI ✅ / 🔜

- AI orchestration (guard → intent → tool → response) ✅
- Offline intent engine + context resolution ✅
- Optional online provider (OpenAI-compatible, consent-gated) ✅
- Local model integration (MediaPipe LLM Inference API / llama.cpp) 🔜 —
  interface + capability checks are in place; no model is bundled. Integration
  steps are documented in `docs/KNOWN_LIMITATIONS.md`.

## Phase 5 — Tools & Memory ✅

Permission-controlled tools, encrypted local memory, reminders, notes, tasks,
approved device integrations.

## Phase 6 — Security & QA ✅ (code) / 🚧 (device)

- Unit/integration/UI/security/offline/voice/error-recovery tests ✅ (in repo,
  run in CI)
- Real-hardware validation 🚧 — must be run by the maintainer on physical
  devices; see `docs/TEST_REPORT.md`.

## Phase 7 — Release 🚧

- Complete Android Studio source project ✅
- Debug APK: build via `./gradlew :app:assembleDebug` or CI artifact ✅
- Release APK/AAB: requires maintainer signing key (`keystore.properties`,
  git-ignored) — instructions in README 🔜 (maintainer action)
- Docs: architecture, privacy, test report, known limitations ✅

## Future (post-v1)

1. On-device LLM via MediaPipe LLM Inference API with model download manager.
2. Porcupine-based wake word (optional, user-supplied key) for better accuracy.
3. Multilingual intent packs (architecture supports locale-parameterised
   patterns).
4. Wear OS / companion quick-reply (out of scope for v1).
