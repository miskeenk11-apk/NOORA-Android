package com.noora.assistant.ui

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.noora.assistant.security.NooraSessionAuthorization
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.noora.assistant.ai.GatewayAiClient
import com.noora.assistant.ai.NooraConversationCoordinator
import com.noora.assistant.core.NooraConnectivity
import com.noora.assistant.memory.NooraMemoryStore
import com.noora.assistant.phone.NooraPhoneControl
import com.noora.assistant.recognition.UserRecognitionManager
import com.noora.assistant.voice.NooraSpeechRecognizer
import com.noora.assistant.voice.NooraTextToSpeech

class MainActivity: AppCompatActivity() {
    private val recognition = UserRecognitionManager()
    private lateinit var listener: NooraSpeechRecognizer
    private lateinit var wakeWord: com.noora.assistant.voice.NooraWakeWordEngine
    private lateinit var speaker: NooraTextToSpeech
    private lateinit var coordinator: NooraConversationCoordinator
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var continuousConversation = false
    private var voiceBusy = false
    private var wakeWordEnabled = false
    private val sessionAuthorization = NooraSessionAuthorization()
    private lateinit var avatar: NooraAvatarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            avatar = NooraAvatarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 420)
            setState("Ready")
        }

        val status = TextView(this).apply {
            textSize = 20f
            setPadding(40, 60, 40, 30)
            text = "NOORA\n\nReady.\nMode: ${NooraConnectivity(this@MainActivity).mode()}"
        }

        speaker = NooraTextToSpeech(this)
        speaker.setStatusListener { message -> runOnUiThread {
            if (message == "Voice ready") status.text = "NOORA\n\nReady.\nVoice: Ready"
        } }
        listener = NooraSpeechRecognizer(this)
        wakeWord = com.noora.assistant.voice.NooraWakeWordEngine(this)
        speaker.setCompletionListener {
            voiceBusy = false
            if (continuousConversation) {
                mainHandler.postDelayed({ startListening(status) }, 350L)
            }
        }

        val gateway = GatewayAiClient(
            gatewayUrl = "https://YOUR-NOORA-GATEWAY.example.com",
            gatewayToken = null
        )
        coordinator = NooraConversationCoordinator(
            NooraConnectivity(this), NooraMemoryStore(this), speaker, gateway, NooraPhoneControl(this)
        ) { message -> runOnUiThread {
            status.text = "NOORA\n\n$message"
            avatar.setState(when {
                message.contains("Listening", true) -> "Listening"
                message.contains("Thinking", true) -> "Thinking"
                message.contains("Speaking", true) -> "Speaking"
                else -> "Ready"
            })
        } }

        val listen = Button(this).apply {
            text = "TALK TO NOORA"
            setOnClickListener {
                continuousConversation = !continuousConversation
                text = if (continuousConversation) "STOP CONVERSATION" else "TALK TO NOORA"
                if (continuousConversation) startListening(status) else listener.cancel()
            }
        }

        val security = Button(this).apply {
            text = "SECURITY: OFF"
            setOnClickListener {
                sessionAuthorization.setRecognitionEnabled(!sessionAuthorization.recognitionEnabled)
                text = if (sessionAuthorization.recognitionEnabled) "SECURITY: ON" else "SECURITY: OFF"
                status.text = if (sessionAuthorization.recognitionEnabled) {
                    "NOORA\n\nSecurity ON\nPlease identify/authorize the current user."
                } else {
                    "NOORA\n\nSecurity OFF\nOwner session active."
                }
            }
        }

        val owner = Button(this).apply {
            text = "I AM OWNER"
            setOnClickListener {
                sessionAuthorization.authorizeOwner()
                status.text = "NOORA\n\nOwner authorized for this session."
            }
        }

        val guest = Button(this).apply {
            text = "AUTHORIZE GUEST"
            setOnClickListener {
                sessionAuthorization.authorizeGuest()
                status.text = "NOORA\n\nAuthorized guest for this session."
            }
        }

        val biometric = Button(this).apply {
            text = "STRONG AUTH TEST"
            setOnClickListener { showBiometricPrompt(status) }
        }

        val wake = Button(this).apply {
            text = "WAKE WORD: OFF"
            setOnClickListener {
                wakeWordEnabled = !wakeWordEnabled
                text = if (wakeWordEnabled) "WAKE WORD: ON" else "WAKE WORD: OFF"
                if (wakeWordEnabled) {
                    continuousConversation = false
                    listen.text = "TALK TO NOORA"
                    listener.cancel()
                    status.text = "NOORA\n\nSay: NOORA"
                    wakeWord.start(
                        onWake = { command ->
                            wakeWord.stop()
                            continuousConversation = true
                            runOnUiThread {
                                listen.text = "STOP CONVERSATION"
                                status.text = "NOORA\n\nAwake"
                                if (command.isBlank()) startListening(status)
                                else {
                                    voiceBusy = true
                                    status.text = "NOORA\n\nYou: $command\n\nThinking..."
                                    coordinator.handleTranscript(command)
                                }
                            }
                        },
                        onState = { state -> runOnUiThread { status.text = "NOORA\n\nWake Word\n$state" } }
                    )
                } else {
                    wakeWord.stop()
                    status.text = "NOORA\n\nReady"
                }
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(avatar)
            addView(status)
            addView(listen)
            addView(wake)
            addView(security)
            addView(owner)
            addView(guest)
            addView(biometric)
        })
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun startListening(status: TextView) {
        if (!continuousConversation || voiceBusy || !listener.isAvailable()) return
        listener.listen(
            languageTag = "en-US",
            onText = { transcript ->
                voiceBusy = true
                runOnUiThread { status.text = "NOORA\n\nYou: $transcript\n\nThinking..."; avatar.setState("Thinking") }
                coordinator.handleTranscript(transcript)
            },
            onError = {
                voiceBusy = false
                if (continuousConversation) {
                    mainHandler.postDelayed({ startListening(status) }, 700L)
                } else {
                    runOnUiThread { status.text = "NOORA\n\nVoice input could not be completed." }
                }
            },
            onState = { state ->
                runOnUiThread { status.text = "NOORA\n\n$state"; avatar.setState(state) }
            }
        )
    }

    private fun showBiometricPrompt(status: TextView) {
        val manager = BiometricManager.from(this)
        val canAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Strong Android authentication is not available on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                sessionAuthorization.authorizeOwner()
                status.text = "NOORA\n\nStrong authentication successful."
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                status.text = "NOORA\n\nAuthentication cancelled or unavailable."
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("NOORA Security")
                .setSubtitle("Confirm that you are authorized to use NOORA")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        )
    }

    override fun onDestroy() {
        continuousConversation = false
        mainHandler.removeCallbacksAndMessages(null)
        coordinator.shutdown()
        listener.destroy()
        wakeWord.destroy()
        speaker.shutdown()
        super.onDestroy()
    }
}
