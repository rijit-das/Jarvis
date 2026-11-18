# JARVIS Mobile

A privacy-first, offline-first personal AI assistant for Android.
Kotlin · Jetpack Compose · Material 3 · Room (SQLCipher) · Hilt · WorkManager.

JARVIS runs on-device by default: voice input/output, wake word, clap
activation, reminders, notes, tasks, memory, and device actions all work
without a network connection. An optional online AI provider is available
behind explicit consent — and is always indicated in the UI.

## Quick start

**Requirements:** Android Studio (Hedgehog or newer), JDK 17, Android SDK
with API 34 platform. Minimum device: Android 8.0 (API 26), 2 GB RAM.

```bash
# Clone, then open the project root in Android Studio and run :app,
# or build from the command line:
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Run unit tests:
./gradlew :app:testDebugUnitTest

# Run instrumented tests (needs an emulator/device):
./gradlew :app:connectedDebugAndroidTest
```

CI (`.github/workflows/android.yml`) runs unit tests + `assembleDebug` on
every push and uploads the debug APK as an artifact.

## What works (v1)

- **Voice:** on-device-preferred speech recognition, TTS with rate/pitch
  control, "Hey JARVIS" wake word (opt-in, foreground-bound), double-clap
  activation (opt-in, validated, configurable).
- **AI brain:** deterministic offline intent engine with context resolution
  ("open that again"), plus an optional OpenAI-compatible online provider
  (API key stored encrypted; consent-gated; offline mode blocks network).
- **Tools (permission-based, confirmed, audited):** time/date, timers,
  reminders (survive reboot), notes, tasks, open app, media control,
  navigation handoff, notification reading (user-enabled), memory
  remember/recall/forget.
- **Memory:** encrypted local storage (SQLCipher + Android Keystore);
  inspect/edit/export/delete in the Memory screen.
- **Security:** tool allowlist, confirmation gates, prompt-injection guard,
  audit log, least-privilege permissions, no secrets in source or logs.
- **Screens:** Home, Chat, Voice, Memory, Model settings, Privacy center,
  Permissions dashboard, Activity history, Diagnostics, Settings.

## What does NOT work yet (honest list)

- No bundled on-device LLM (see `docs/KNOWN_LIMITATIONS.md` — the intent
  engine is the v1 brain; an LLM integration path is documented).
- Wake-word accuracy/battery varies by device and speech recognizer.
- Release signing requires your own keystore (`keystore.properties`,
  git-ignored) — see below.

## Release builds

```properties
# keystore.properties (project root, git-ignored)
storeFile=path/to/your.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

Then `./gradlew :app:assembleRelease` (or `bundleRelease` for an AAB).
Without `keystore.properties`, release builds are unsigned — debug builds
always work.

## Documentation

| Doc | Contents |
|-----|----------|
| `docs/PRD.md` | Product requirements |
| `docs/ARCHITECTURE.md` | System architecture & package map |
| `docs/PERMISSION_MATRIX.md` | Every permission, why, when |
| `docs/THREAT_MODEL.md` | Threats & mitigations |
| `docs/COMPATIBILITY.md` | Device requirements & capability tiers |
| `docs/OFFLINE_ONLINE_MATRIX.md` | What works offline vs online |
| `docs/ROADMAP.md` | Phases & status |
| `docs/TEST_REPORT.md` | Test strategy & results |
| `docs/KNOWN_LIMITATIONS.md` | Honest limitations |

## Project structure

```
app/src/main/java/com/jarvis/
├── ui/          Compose screens, ViewModels, theme, navigation
├── ai/          intent engine, context, model manager, providers, orchestrator
├── tools/       tool contract, registry, executor, concrete tools
├── voice/       STT, TTS, wake word, clap detection, voice session
├── memory/      user-controlled memory facade
├── data/        Room (SQLCipher), DataStore, repositories
├── core/        security, permissions, network, DI, common
├── workers/     reminders, cleanup
└── notifications/ optional notification listener
```

## Privacy promise

- Offline by default. No audio upload without explicit consent.
- No raw microphone recordings stored. No hidden recording.
- No secrets in source code. API keys in Keystore-backed encrypted storage.
- Personal data excluded from cloud backups.
- Every tool execution is audit-logged (metadata only, no secrets).
