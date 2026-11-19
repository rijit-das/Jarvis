# JARVIS Mobile — Permission Matrix

Principle: **least privilege**. Every permission is requested in context, with
rationale, and can be revoked at any time from the Permissions dashboard or
system settings. Nothing is requested silently at startup.

| Permission | Type | Used by | Why | Requested when | Revocable |
|------------|------|---------|-----|----------------|-----------|
| `RECORD_AUDIO` | Runtime | Voice input, wake word, clap | Microphone access for STT/detection | First voice use | Yes |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Reminders, voice-session FGS | Show reminder + FGS notification | First reminder / voice session | Yes |
| `SCHEDULE_EXACT_ALARM` | Special | Reminders | Exact-time reminder delivery | First reminder (with fallback to inexact) | Yes |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Install-time + runtime type | Voice session | Keep mic session alive while listening | Voice session start | Yes (stop session) |
| `INTERNET` | Install-time | Online provider only | HTTPS API calls | Only when user enables online mode | Yes (offline mode blocks use) |
| `ACCESS_NETWORK_STATE` | Install-time | Connectivity monitor | Detect online/offline state | App start (no prompt) | N/A |
| `WAKE_LOCK` | Install-time | Voice session | Prevent sleep during active listening | Voice session | N/A |
| `RECEIVE_BOOT_COMPLETED` | Install-time | Reminder rescheduling | Re-register reminders after reboot | Install (no prompt) | N/A |
| `VIBRATE` | Install-time | Reminder feedback | Haptic cue on reminder | Install (no prompt) | N/A |

## Special access (never requested silently)

| Access | How it works |
|--------|--------------|
| Notification Listener | User must enable JARVIS in system Settings → Notifications → Notification access. The app links to that settings screen; it cannot enable itself. Required only for the "read notifications" tool. |
| Exact alarms | User can revoke in system settings; JARVIS falls back to inexact (WorkManager) delivery and says so. |

## Explicitly NOT requested

- Location (navigation uses a handoff intent to the maps app; JARVIS never
  reads location itself).
- Contacts, SMS, Call Log, Camera, Storage (except user-initiated export via
  Storage Access Framework).
- Accessibility Service.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (never; users may do it manually).

## Permission flows

1. User taps a feature → rationale dialog (what + why) → system prompt.
2. Denial → feature degrades gracefully with an explanation; Settings link
   offered. No nagging loops.
3. "Don't ask again" → Permissions dashboard shows the state and how to fix it.
