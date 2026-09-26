# NOORA Secure AI Gateway

This gateway keeps the provider API key on the server, never in the Android APK.

## Configure
Set environment variables:
- OPENAI_API_KEY
- OPENAI_MODEL (default: gpt-5.6-luna)
- NOORA_GATEWAY_TOKEN (required for production)

## Endpoints
- GET /health
- POST /v1/chat

POST /v1/chat body:
{"messages":[{"role":"user","content":"Hello NOORA"}]}

The Android app should send only to this gateway URL. Do not put the provider key in Android source, resources, BuildConfig, or the APK.
