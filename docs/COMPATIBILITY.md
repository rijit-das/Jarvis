# JARVIS Mobile — Device Compatibility

## 1. Minimum requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Android version | 8.0 (API 26) | 12+ (API 31+) |
| RAM | 2 GB | 4 GB+ |
| Free storage | 200 MB (app + DB) | 1 GB+ |
| Microphone | Required for voice features | — |
| Speech services | Any installed `SpeechRecognizer` (Google app or on-device) | On-device pack installed |
| TTS engine | Any installed engine | System default |

## 2. Capability tiers (`ModelManager`)

| Tier | Heuristic | Behaviour |
|------|-----------|-----------|
| **Low** | < 3 GB RAM or < 2 GB free storage | Intent engine only; no wake-word background listening; clap detection available in foreground; online provider optional |
| **Medium** | 3–6 GB RAM | Full offline feature set; wake-word foreground session |
| **High** | 6 GB+ RAM | Full feature set; eligible for future on-device LLM (roadmap) |

Tier detection uses `ActivityManager.MemoryInfo` (total memory), available
storage, and Android version. The tier is shown in Diagnostics and Model
settings. JARVIS never attempts to load a model the device cannot support.

## 3. Android version notes

- **API 26–30:** foreground service types not enforced; mic indicator absent
  (JARVIS provides its own indicator).
- **API 31+:** microphone privacy indicators; `POST_NOTIFICATIONS` runtime
  permission; exact-alarm permission model.
- **API 33+:** `POST_NOTIFICATIONS` required for reminders/FGS notification.
- **API 34+:** foreground service type must be declared (`microphone`) and
  `FOREGROUND_SERVICE_MICROPHONE` permission required.

## 4. Speech services

Offline STT quality depends on the installed recognizer. JARVIS requests
`EXTRA_PREFER_OFFLINE` and reports recognizer availability in Diagnostics. If
no recognizer is installed, voice input is disabled with an explanation and
text input remains fully functional.

## 5. Testing matrix (target)

- Emulator: API 26, 30, 34 (low-memory + standard profiles).
- Physical: at least one low-end (2–3 GB RAM) and one mid-range device.
- Airplane-mode pass for all offline features.
