# NOORA V1 — Calls, Contacts & Auto Answer Foundation

This stage adds a permission-based Android foundation for:
- calling a saved contact by name or a typed phone number
- contact lookup through Android ContactsProvider
- answering and ending calls where Android/telecom permissions allow
- configurable Auto Answer (default OFF) with a 1–60 second delay
- an InCallService foundation for Android-supported call handling
- requesting the default dialer role when the device requires it for deeper telecom control

## Important Android limits
NOORA does not bypass Android security. Some call-control behavior depends on Android version, device/OEM, granted permissions, and whether NOORA is allowed to act as the default phone/dialer app. Auto Answer is OFF by default.

## Permissions
The manifest includes CALL_PHONE, READ_CONTACTS, READ_PHONE_STATE, ANSWER_PHONE_CALLS and READ_CALL_LOG. Android runtime permission prompts are still required where applicable.

## Not claimed as tested
This project has not been compiled or tested on a physical Android phone in this environment. Treat this as an implementation foundation until installed and tested on the target phone.
