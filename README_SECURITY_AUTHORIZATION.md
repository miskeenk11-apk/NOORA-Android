# NOORA V1 — Recognition + Security + User Authorization Foundation

This stage integrates a privacy-first session authorization layer with the existing voice, wake-word, phone-control, accessibility, calls, contacts and auto-answer foundations.

## Included
- Explicit Security ON/OFF session control.
- Owner authorization for the current session.
- Authorized guest session state.
- Unknown-user state when recognition/security mode is enabled.
- Android BiometricPrompt / device-credential strong-authentication foundation.
- Recognition remains a convenience signal; it is not treated as a sole high-security authenticator.

## Privacy boundary
This build does **not** claim covert or automatic facial identification. Camera/recognition ML can be connected later through `UserRecognitionManager` with explicit Android camera permission and visible user controls.

## Important
Sensitive operations should require Android biometric/PIN confirmation. Phone and Accessibility capabilities remain limited by Android permissions, roles and OS behavior.
