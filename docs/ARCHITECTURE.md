# JARVIS Mobile — System Architecture

## 1. Overview

Single Gradle module (`:app`) with strict package-layer separation. The package
structure *is* the module map; each layer has one-way dependencies:

```
ui ──► ai (orchestrator) ──► tools ──► data
 │           │                  │
 │           ▼                  ▼
 │        voice              memory
 ▼
core (security, permissions, network, di, common)
```

- **Presentation (`ui`)** — Jetpack Compose screens + ViewModels. No business
  logic; observes StateFlow, emits events.
- **Application/domain (`ai`, `tools`, `memory`, `voice`)** — pure Kotlin,
  Android APIs behind interfaces. Unit-testable on the JVM.
- **Data (`data`)** — Room (SQLCipher-encrypted), DataStore, EncryptedSharedPreferences.
- **Platform (`core`)** — DI (Hilt), permissions, connectivity, security primitives.

## 2. Package map

| Package | Responsibility |
|---------|----------------|
| `com.jarvis.ui.*` | Screens, ViewModels, theme, navigation, shared components |
| `com.jarvis.ai.intent` | Offline NLU: intent classification, entity extraction |
| `com.jarvis.ai.context` | Conversation context + reference resolution |
| `com.jarvis.ai.model` | Device capability tiers, model availability |
| `com.jarvis.ai.provider` | `AiProvider` abstraction; offline + online implementations |
| `com.jarvis.ai.orchestrator` | Request pipeline: guard → intent → tool → response |
| `com.jarvis.tools` | Tool contract, registry, executor, concrete tools |
| `com.jarvis.voice` | STT, TTS, wake-word, clap detection, voice session, app-scoped activation manager |
| `com.jarvis.memory` | Memory categories, retrieval, retention |
| `com.jarvis.data.local` | Room entities/DAOs/database (SQLCipher) |
| `com.jarvis.data.datastore` | Settings + secure settings repositories |
| `com.jarvis.data.repository` | Repository facades over DAOs |
| `com.jarvis.core.security` | Keystore, audit log, prompt-injection guard |
| `com.jarvis.core.permissions` | Permission state + request helpers |
| `com.jarvis.core.network` | Connectivity monitor, offline-mode enforcement |
| `com.jarvis.workers` | Reminder scheduling, cleanup |
| `com.jarvis.notifications` | Optional NotificationListener (user-enabled) |

## 3. Request pipeline (`AssistantOrchestrator`)

```
User input (text or STT transcript)
  → PromptInjectionGuard (sanitise external content)
  → IntentEngine.classify()        (offline, deterministic)
  → Context resolution             ("open that again", follow-ups)
  → ToolExecutor.execute()         (allowlist → permission → confirm → timeout → audit)
  → ResponseComposer               (spoken + displayed text)
  → AuditLogger                    (every step, no secrets)
```

If the intent is unknown and online mode is enabled *and* consent is granted,
the request may be routed to the online provider. Otherwise JARVIS explains its
limitation and suggests supported commands. It never fabricates results.

## 4. Tool contract

Every tool implements `Tool` with: `name`, `description`, `inputSchema`,
`requiredPermission`, `requiresConfirmation`, `timeoutMs`, `execute()`.
`ToolRegistry` enforces an allowlist; `ToolExecutor` applies permission checks,
confirmation, timeouts, and audit logging. Tools never receive shell access.

## 5. Voice architecture

- **STT:** `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE`; partial results;
  graceful degradation when no recognizer is installed.
- **TTS:** `TextToSpeech` with user-configurable rate/voice; interruptible.
- **Wake word:** `WakeWordDetector` interface; default implementation uses the
  on-device recognizer in a foreground-service session. Opt-in, visible
  indicator, stop control. See `KNOWN_LIMITATIONS.md`.
- **Clap:** `ClapDetector` analyses PCM energy + zero-crossing rate; requires a
  validated double-clap pattern; configurable threshold; cooldown anti-trigger.
- **Session:** `VoiceSessionManager` owns the mic lifecycle; mic is released
  whenever the session is idle.

## 6. Memory architecture

- Room tables: `memories`, `notes`, `tasks`, `reminders`, `conversation_turns`,
  `audit_log`. Database encrypted with SQLCipher; passphrase in
  Keystore-backed EncryptedSharedPreferences.
- Retrieval is structured (category + keyword search), never "dump the whole
  history into the prompt".
- Memory is opt-in per category; nothing is silently persisted. Session context
  is in-memory only.

## 7. Security architecture

See `docs/THREAT_MODEL.md`. Key points: least-privilege permissions, Keystore
key management, tool allowlist, confirmation gates, offline-mode network block,
prompt-injection guard, secret-free logging.

## 8. Concurrency & performance

- Coroutines + Flow throughout; `DispatcherProvider` abstracts dispatchers for
  testability.
- Room/DataStore on IO dispatcher; STT/TTS callbacks marshalled to main.
- WorkManager for reminder delivery fallback and periodic cleanup.
- Foreground service only for the voice session (microphone type), with
  persistent notification and stop control.

## 9. Build configuration

- Kotlin 2.0.20, AGP 8.5.2, Gradle 8.7, Java 17.
- `compileSdk/targetSdk 34`, `minSdk 26`.
- Compose BOM 2024.09.00, Material 3, Hilt 2.51.1, Room 2.6.1.
- Version catalog: `gradle/libs.versions.toml`.
