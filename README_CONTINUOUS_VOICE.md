# NOORA Continuous Natural Voice Conversation

This stage adds a controlled conversation loop on top of the existing real speech and gateway foundation.

Flow:

1. User taps **TALK TO NOORA**.
2. NOORA listens.
3. Speech is converted to text.
4. Existing online/offline conversation coordinator handles the transcript.
5. NOORA speaks the response.
6. After speech finishes, NOORA waits briefly and listens again.
7. User can press **STOP CONVERSATION** to stop the loop.

The loop does not keep the microphone open while NOORA is speaking, which helps prevent NOORA from immediately recognizing her own voice.

This stage does not claim always-on background listening or a wake-word system. Those are separate later features.
