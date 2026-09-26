# NOORA Wake Word Foundation

This stage adds a user-controlled **WAKE WORD** mode.

- Tap `WAKE WORD: OFF` to turn it on.
- NOORA listens for the phrase **"NOORA"** or **"نورا"**.
- Saying `NOORA` wakes the assistant and starts the normal conversation flow.
- Saying `NOORA, what time is it?` can pass the remaining phrase directly to the conversation coordinator.
- Turning Wake Word off stops the listener.

## Important implementation note

This foundation uses Android's speech recognizer as the wake-phrase detector. It is **not** a dedicated low-power hardware hotword engine and should not be represented as one. Continuous speech recognition can consume battery and behavior depends on the installed Android recognition service.

A later production upgrade can replace `NooraWakeWordEngine` with a dedicated on-device hotword engine without changing the rest of NOORA's conversation architecture.
