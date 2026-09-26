# NOORA V1 — Final Integration Foundation

This package consolidates the NOORA V1 foundations into one Android project and adds a lightweight local avatar/UI state layer.

## Integrated foundations
- AI gateway client + online/offline routing
- Urdu/English voice input and TTS
- Continuous conversation loop
- NOORA wake-word foundation
- Local phone commands
- Accessibility phone-control foundation
- Calls, contacts and auto-answer foundation
- User recognition/session authorization foundation
- Biometric/device-credential confirmation foundation
- Persistent local conversation memory
- Lightweight NOORA avatar with Ready / Listening / Thinking / Speaking states

## Security and platform boundaries
NOORA uses Android permissions, roles and accessibility services. It does not bypass Android security. Sensitive operations should use explicit confirmation/biometric or device credential where appropriate.

The wake-word implementation is a foundation based on Android speech recognition; it is not claimed to be a production-grade low-power always-on hotword engine.

## Current deployment requirements
1. Configure and deploy the HTTPS NOORA gateway.
2. Put the provider API key only on the gateway server, never in the APK.
3. Replace the placeholder gateway URL in `MainActivity.kt`.
4. On the Android phone, grant microphone/camera permissions and enable Accessibility/telecom roles where required.
5. Build and test on a real Android device. This environment does not contain a working Gradle/Android SDK build toolchain, so an APK is not claimed as compiled here.

## Version
NOORA V1 Final Integration Foundation — 0.1.0
