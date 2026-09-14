# CallCheck — Android scaffold

A real Android Studio project (Kotlin + Jetpack Compose), not a simulation.
Every screen calls a genuine Android/Firebase API. It is **not buildable
as-is into an APK** — it needs your own Firebase project and a few hours
in Android Studio — but nothing in it is fake.

## To actually run this

1. Open the folder in Android Studio (Giraffe+ recommended).
2. Create a Firebase project at console.firebase.google.com, enable
   **Authentication → Phone**, and download `google-services.json` into
   `app/`.
3. Add your debug + release SHA-1 fingerprints in the Firebase console
   (`./gradlew signingReport`) — Phone Auth needs this for Play
   Integrity / reCAPTCHA fallback verification.
4. Build and run on a real device (phone/SMS features don't work in the
   emulator without extra setup).
5. To test spam-call screening: Settings → Apps → Default apps →
   Caller ID & spam blocking → select CallCheck.
6. To test the voice check: it only runs during an active call, after
   the user explicitly taps "Check this call" and consents to
   speakerphone mode.

## What's genuinely real here

| Feature | Status |
|---|---|
| Phone number entry + real SMS OTP | ✅ Real, via Firebase Phone Auth |
| Contacts list | ✅ Real, via `ContactsContract` |
| Placing calls | ✅ Real, via `ACTION_CALL` intent |
| Spam/scam number screening before pickup | ✅ Real, via `CallScreeningService` |
| Live voice-authenticity check | ⚠️ Real code, but fundamentally limited — see below |

## The wall Android puts up, and why the voice check is built the way it is

Android does not let third-party apps read the raw audio of a phone call.
`AudioRecord` with a call-audio source (`VOICE_CALL`, `VOICE_DOWNLINK`)
requires the `CAPTURE_AUDIO_OUTPUT` permission, which is signature/system
level — Google does not grant it to regular apps, on any current Android
version. `CallScreeningService` (used above for spam blocking) only ever
sees the phone number and metadata, never audio.

The only remaining option — used here — is: put the call on
speakerphone, and record with the ordinary microphone, which then picks
up whatever is playing through the speaker. This is what most
call-recording apps on the Play Store actually do under the hood. It
means:

- The user must explicitly opt in per call (this is implemented as a
  disclosed action, not a silent background feature — keep it that way).
- Audio quality is worse than a direct stream, and it'll pick up the
  user's own voice and room noise too.
- The `spectralFlatness()` heuristic in `VoiceCheckService.kt` is
  illustrative signal processing, not a validated deepfake/voice-clone
  detector. Getting real accuracy means training an actual classifier
  (a TFLite model trained on labeled real vs. AI-generated speech
  samples) and swapping it in — a genuine ML project on its own, not
  something that drops in as a library.

## Before shipping this to real users

- **Consent/recording law**: recording a phone call — even just your own
  side, even for your own protection — has legal requirements that vary
  by country and, within India, by state. Check what applies to you.
- **Play Store policy**: Google has tightened rules around call-related
  audio access several times; review current policy before publishing.
- **iOS**: none of the call-audio pieces above are portable to iPhone.
  Apple's sandboxing blocks third-party call-audio access outright —
  this feature would need to be Android-only, full stop.
