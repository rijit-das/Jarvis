# JARVIS Mobile — Known Limitations (honest list)

Nothing below is hidden. If a feature is limited, this document says so.

## 1. Wake word ("Hey JARVIS")

- Android has no built-in offline hotword API. JARVIS's implementation uses the
  device's `SpeechRecognizer` in a foreground-service session, listening for the
  phrase. Consequences:
  - Accuracy and battery cost vary by device and recognizer.
  - Continuous background listening is **not** claimed to be reliable on all
    Android versions; it is opt-in and foreground-bound by design.
  - If the installed recognizer lacks an offline pack, wake-word may use the
    recognizer's default path. JARVIS requests offline preference and discloses
    this in the Privacy screen.
- Roadmap: optional Porcupine integration (user-supplied key) for a dedicated
  on-device hotword model.

## 2. On-device LLM

- **Not bundled in v1.** `ModelManager` reports `NotInstalled` and JARVIS falls
  back to the deterministic intent engine. This is deliberate: shipping a
  multi-GB model inside the APK is impractical, and not every phone can run one.
- Integration path (documented, not implemented): add
  `com.google.mediapipe:tasks-genai`, implement `LocalLlmProvider` behind the
  existing `AiProvider` interface, gate on `CapabilityTier.High`, and download
  the model via a user-initiated, resumable download with checksum verification.

## 3. Speech recognition quality

- Depends on the installed recognizer and its offline language pack. Accents,
  noise, and pauses are handled as well as the platform recognizer allows;
  JARVIS surfaces partial results and errors instead of guessing.

## 4. Reminders

- Exact alarms require `SCHEDULE_EXACT_ALARM`; if revoked, JARVIS falls back to
  inexact delivery (WorkManager) and tells the user. Doze mode may delay
  inexact reminders.

## 5. Notification reading

- Requires the user to manually enable JARVIS in system notification-access
  settings. JARVIS cannot and does not enable it programmatically.

## 6. Online provider

- Only OpenAI-compatible chat-completions endpoints are supported in v1.
- Responses are labelled as online-generated; JARVIS does not verify their
  factual accuracy.

## 7. Build verification

- This codebase was authored without an Android SDK in the authoring
  environment. It is validated by static checks and CI (`.github/workflows/
  android.yml` runs unit tests + `assembleDebug` on every push). First
  maintainer build should be treated as the initial verification gate; see
  `docs/TEST_REPORT.md`.
