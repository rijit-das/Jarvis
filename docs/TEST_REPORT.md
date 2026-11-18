# JARVIS Mobile — Test Report

## 1. Test strategy

| Layer | Framework | Location |
|-------|-----------|----------|
| Unit tests | JUnit 4 + kotlinx-coroutines-test | `app/src/test/` |
| Instrumented tests | AndroidX Test + Compose UI Test | `app/src/androidTest/` |
| CI | GitHub Actions (JDK 17, Android SDK) | `.github/workflows/android.yml` |

## 2. Coverage by requirement

| Area | Tests |
|------|-------|
| Intent engine (all intents, entities, clarification) | `IntentEngineTest` |
| Context resolution ("open that again", follow-ups) | `ConversationContextTest` |
| Duration/time parsing ("in 5 minutes", "at 7 pm") | `DurationParserTest` |
| Clap detector (true claps, noise rejection, double-clap validation) | `ClapDetectorTest` |
| Tool registry (allowlist, unknown tool rejection) | `ToolRegistryTest` |
| Tool executor (permission gate, confirmation gate, timeout) | `ToolExecutorTest` |
| Prompt-injection guard | `PromptInjectionGuardTest` |
| Model manager capability tiers | `ModelManagerTest` |
| Memory manager (CRUD, categories, retention) | `MemoryManagerTest` |
| Orchestrator (offline routing, online consent gate) | `AssistantOrchestratorTest` |
| UI smoke (home renders, navigation) | `UiSmokeTest` (instrumented) |

## 3. Manual test checklist (maintainer, physical device)

- [ ] Microphone permission denied → graceful fallback to text.
- [ ] Microphone unavailable (no recognizer) → explanation, text still works.
- [ ] Airplane mode → all offline features pass; online provider blocked.
- [ ] Invalid API key → clear error, no crash, no key logged.
- [ ] Low storage → diagnostics warn; app remains usable.
- [ ] Background restrictions (battery optimisation) → reminder fallback works.
- [ ] Interrupted speech (stop button / barge-in) → TTS stops immediately.
- [ ] False wake-word activation → stop control ends session instantly.
- [ ] Accidental clap activation → double-clap validation + cooldown prevent it.
- [ ] Process death during voice session → app recovers to idle state.
- [ ] Reminder fires at exact time; survives reboot (rescheduled).

## 4. Results

- **Automated:** run in CI on every push (`testDebugUnitTest` + `assembleDebug`).
  Results are visible in the GitHub Actions run for the commit.
- **Static validation at authoring time:** brace/balance checks, manifest/code
  consistency, and dependency-version verification (see commit history).
- **Device validation:** pending maintainer run of §3. This report will be
  updated with device models, Android versions, and pass/fail results once run.

> Honesty note: no test result is claimed here that has not been produced by
> running the suite. If CI is red, the release is not ready — fix first.
