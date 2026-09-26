# NOORA V1 — Live AI Gateway Foundation

This stage connects the Android NOORA app to a server-side AI gateway. The provider API key remains on the server and is never placed in the APK.

## What is included
- Android `GatewayAiClient`
- Online/offline routing through `NooraConversationCoordinator`
- Recent local memory context passed to the gateway
- Node.js secure gateway with `/health` and `/v1/chat`
- Environment-based provider key/model configuration
- Provider-neutral Android boundary so the provider can be replaced later

## Provider connection
The gateway uses OpenAI's Responses API. OpenAI's current documentation recommends keeping the API key in an environment variable on the server rather than in client applications. citeturn1search0

## Before live use
1. Deploy the `gateway/` directory to a server with HTTPS.
2. Set `OPENAI_API_KEY` on the server only.
3. Set `OPENAI_MODEL` to the desired model.
4. Set a strong `NOORA_GATEWAY_TOKEN` or replace it with proper per-device authentication before production.
5. Replace the placeholder gateway URL in `MainActivity.kt` with the deployed HTTPS gateway URL.

Do not put the provider API key in Android source, resources, BuildConfig, or APK.

## Current limitation
The project contains the live connection code, but it is not claiming live provider operation until a real gateway URL and provider key are configured and tested.
