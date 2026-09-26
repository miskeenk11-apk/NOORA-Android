# NOORA — Real Phone Control Foundation

This stage adds safe, Android-supported local phone actions before cloud AI processing.

Supported command examples:
- Open Settings / Wi-Fi / Bluetooth
- Open Camera
- Open browser
- Open WhatsApp / YouTube when installed
- Open Phone / Messages
- Volume up/down, mute/unmute

The commands are intercepted locally, so they can work without internet.

Important limitation: this is a foundation, not unrestricted phone control. Android protected actions still require official APIs, user permissions, roles, or Accessibility where appropriate. Back/Home, arbitrary tap/type/scroll, and protected settings are intentionally not claimed here and will be added through the proper Android mechanisms in later stages.
