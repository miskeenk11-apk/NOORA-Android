package com.noora.assistant.ui

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageButton
import android.widget.FrameLayout
import android.view.Gravity
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
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
    private lateinit var avatar: NooraAvatarView
    private lateinit var statusView: TextView
    private lateinit var moreButton: ImageButton
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var continuousConversation = false
    private var voiceBusy = false
    private var wakeWordEnabled = false
    private var listeningRestartPending = false
    private var silentMode = false
    private val sessionAuthorization = NooraSessionAuthorization()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            textSize = 20f
            setPadding(40, 60, 40, 30)
            text = "NOORA\n\nReady.\nMode: ${NooraConnectivity(this@MainActivity).mode()}"
        }

        avatar = NooraAvatarView(this) { action ->
            when (action) {
                NooraAvatarView.Action.VOICE -> {
                    if (continuousConversation) {
                        continuousConversation = false
                        listeningRestartPending = false
                        voiceBusy = false
                        mainHandler.removeCallbacksAndMessages(null)
                        if (::listener.isInitialized) listener.cancel()
                        statusView.text = "NOORA\n\nReady"
                        avatar.setState("Ready")
                    } else if (::listener.isInitialized) {
                        if (wakeWordEnabled) wakeWord.stop()
                        continuousConversation = true
                        listeningRestartPending = false
                        startListening(statusView)
                    }
                }
                NooraAvatarView.Action.SETTINGS -> {
                    if (::moreButton.isInitialized) moreButton.performClick()
                }
                NooraAvatarView.Action.EXIT -> finishAndRemoveTask()
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 420)
            setState("Ready")
        }

        speaker = NooraTextToSpeech(this)
        speaker.setStatusListener { message -> runOnUiThread {
            if (message == "Voice ready") statusView.text = "NOORA\n\nReady.\nVoice: Ready"
        } }

        // Opening greeting: show NOORA's adab/salute state, then greet by time and ask what the user would like.
        avatar.setState("Salute")
        speakTimeBasedGreeting()
        listener = NooraSpeechRecognizer(this)
        wakeWord = com.noora.assistant.voice.NooraWakeWordEngine(this)

        speaker.setCompletionListener {
            voiceBusy = false

            if (continuousConversation) {
                scheduleListening(statusView, 500L)
            } else if (wakeWordEnabled) {
                wakeWord.start(
                    onWake = { command ->
                        wakeWord.stop()

                        runOnUiThread {
                            statusView.text = "NOORA\n\nAwake"
                            avatar.setState("Ready")

                            if (command.isBlank()) {
                                continuousConversation = true
                                voiceBusy = false

                                if (!silentMode) {
                                    statusView.text = "NOORA\n\nListening for your command..."
                                    avatar.setState("Listening")
                                }

                                startListening(statusView)
                            } else {
                                continuousConversation = false

                                if (handleSilentCommand(command, statusView)) {
                                    return@runOnUiThread
                                }

                                voiceBusy = true
                                statusView.text = "NOORA\n\nYou: $command\n\nThinking..."
                                avatar.setState("Thinking")
                                coordinator.handleTranscript(command)
                            }
                        }
                    },
                    onState = { _ ->
                        runOnUiThread {
                            statusView.text = "NOORA\n\nSay: Hey JARVIS"
                            avatar.setState("Ready")
                        }
                    }
                )
            }
        }

        val gateway = GatewayAiClient(
            gatewayUrl = "https://noora-gateway-stc7.vercel.app",
            gatewayToken = null
        )

        coordinator = NooraConversationCoordinator(
            NooraConnectivity(this),
            NooraMemoryStore(this),
            speaker,
            gateway,
            NooraPhoneControl(this)
        ) { message -> runOnUiThread {
            statusView.text = "NOORA\n\n$message"
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

                text = if (continuousConversation) {
                    "STOP CONVERSATION"
                } else {
                    "TALK TO NOORA"
                }

                if (continuousConversation) {
                    listeningRestartPending = false
                    startListening(statusView)
                } else {
                    listeningRestartPending = false
                    voiceBusy = false
                    mainHandler.removeCallbacksAndMessages(null)
                    listener.cancel()

                    statusView.text = "NOORA\n\nReady"
                    avatar.setState("Ready")
                }
            }
        }

        val security = Button(this).apply {
            text = "SECURITY: OFF"

            setOnClickListener {
                sessionAuthorization.setRecognitionEnabled(!sessionAuthorization.recognitionEnabled)

                text = if (sessionAuthorization.recognitionEnabled) {
                    "SECURITY: ON"
                } else {
                    "SECURITY: OFF"
                }

                statusView.text = if (sessionAuthorization.recognitionEnabled) {
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
                statusView.text = "NOORA\n\nOwner authorized for this session."
            }
        }

        val guest = Button(this).apply {
            text = "AUTHORIZE GUEST"

            setOnClickListener {
                sessionAuthorization.authorizeGuest()
                statusView.text = "NOORA\n\nAuthorized guest for this session."
            }
        }

        val biometric = Button(this).apply {
            text = "STRONG AUTH TEST"
            setOnClickListener {
                showBiometricPrompt(statusView)
            }
        }

        val wake = Button(this).apply {
            text = "WAKE WORD: OFF"

            setOnClickListener {
                wakeWordEnabled = !wakeWordEnabled

                text = if (wakeWordEnabled) {
                    "WAKE WORD: ON"
                } else {
                    "WAKE WORD: OFF"
                }

                if (wakeWordEnabled) {
                    continuousConversation = false
                    listeningRestartPending = false
                    voiceBusy = false
                    listen.text = "TALK TO NOORA"
                    mainHandler.removeCallbacksAndMessages(null)
                    listener.cancel()

                    statusView.text = "NOORA\n\nSay: Hey JARVIS"

                    wakeWord.start(
                        onWake = { command ->
                            wakeWord.stop()
                            continuousConversation = true

                            runOnUiThread {
                                listen.text = "STOP CONVERSATION"
                                statusView.text = "NOORA\n\nAwake"

                                if (command.isBlank()) {
                                    continuousConversation = true
                                    voiceBusy = true
                                    statusView.text = "NOORA\n\nSpeaking..."
                                    avatar.setState("Speaking")
                                    speaker.speak("Ji, boliye.", "ur")
                                } else {
                                    continuousConversation = false
                                    voiceBusy = true
                                    statusView.text = "NOORA\n\nYou: $command\n\nThinking..."
                                    avatar.setState("Thinking")
                                    coordinator.handleTranscript(command)
                                }
                            }
                        },
                        onState = { state ->
                            runOnUiThread {
                                statusView.text = "NOORA\n\nWake Word\n$state"
                            }
                        }
                    )
                } else {
                    wakeWord.stop()
                    listeningRestartPending = false
                    voiceBusy = false
                    mainHandler.removeCallbacksAndMessages(null)
                    statusView.text = "NOORA\n\nReady"
                    avatar.setState("Ready")
                }
            }
        }

        // Full-screen NOORA: controls are kept in the existing buttons but exposed
        // only through the three-dot menu so the avatar remains the main screen.
        moreButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_more)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "NOORA options"
            setPadding(18, 18, 18, 18)
            setOnClickListener { anchor ->
                PopupMenu(this@MainActivity, anchor).apply {
                    menu.add(Menu.NONE, 1, 1, "TALK TO NOORA")
                    menu.add(Menu.NONE, 2, 2, "WAKE WORD: OFF")
                    menu.add(Menu.NONE, 3, 3, "SECURITY: OFF")
                    menu.add(Menu.NONE, 4, 4, "I AM OWNER")
                    menu.add(Menu.NONE, 5, 5, "AUTHORIZE GUEST")
                    menu.add(Menu.NONE, 6, 6, "STRONG AUTH TEST")

                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            1 -> listen.performClick()
                            2 -> {
                                item.title = if (wakeWordEnabled) {
                                    "WAKE WORD: ON"
                                } else {
                                    "WAKE WORD: OFF"
                                }
                                wake.performClick()
                            }
                            3 -> {
                                item.title = if (sessionAuthorization.recognitionEnabled) {
                                    "SECURITY: ON"
                                } else {
                                    "SECURITY: OFF"
                                }
                                security.performClick()
                            }
                            4 -> owner.performClick()
                            5 -> guest.performClick()
                            6 -> biometric.performClick()
                        }
                        true
                    }

                    setOnDismissListener { }
                    show()
                }
            }
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                avatar,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                moreButton,
                FrameLayout.LayoutParams(64, 64).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = 18
                    marginEnd = 12
                }
            )
        }

        setContentView(root)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS
            )
        )
    }

    private fun speakTimeBasedGreeting() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 0..4 -> "Good night."
            in 5..11 -> "Good morning."
            in 12..16 -> "Good afternoon."
            in 17..20 -> "Good evening."
            else -> "Good night."
        }

        mainHandler.postDelayed({
            speaker.speak(
                "Assalam-o-Alaikum. $greeting Ji, aap kya pasand karenge.",
                "en"
            )
            mainHandler.postDelayed({
                avatar.setState("Ready")
            }, 5000L)
        }, 700L)
    }

    private fun startListening(statusView: TextView) {
        if (!continuousConversation || voiceBusy || listeningRestartPending || !listener.isAvailable()) {
            return
        }

        listeningRestartPending = false
        voiceBusy = true

        listener.listen(
            languageTag = "en-US",

            onText = { transcript ->
                voiceBusy = true

                runOnUiThread {
                    if (handleSilentCommand(transcript, statusView)) {
                        return@runOnUiThread
                    }

                    statusView.text = "NOORA\n\nYou: $transcript\n\nThinking..."
                    avatar.setState("Thinking")
                    coordinator.handleTranscript(transcript)
                }
            },

            onError = {
                voiceBusy = false

                if (continuousConversation) {
                    scheduleListening(statusView, 1000L)
                } else {
                    runOnUiThread {
                        statusView.text = "NOORA\n\nVoice input could not be completed."
                        avatar.setState("Ready")
                    }
                }
            },

            onState = { state ->
                runOnUiThread {
                    statusView.text = "NOORA\n\n$state"

                    avatar.setState(
                        when {
                            state.contains("Listening", true) -> "Listening"
                            state.contains("Processing", true) -> "Thinking"
                            else -> state
                        }
                    )
                }
            }
        )
    }

    private fun handleSilentCommand(command: String, statusView: TextView): Boolean {
        val normalized = command.lowercase(java.util.Locale.ROOT).trim()

        val isSilentCommand =
            normalized == "silent" ||
            normalized == "be silent" ||
            normalized == "chup" ||
            normalized == "chup ho jao" ||
            normalized == "chup hojao" ||
            normalized == "khamosh" ||
            normalized == "khamosh ho jao" ||
            normalized == "خاموش" ||
            normalized == "چپ" ||
            normalized == "چپ ہو جاؤ"

        if (!isSilentCommand) return false

        silentMode = true
        continuousConversation = false
        voiceBusy = false
        listeningRestartPending = false
        mainHandler.removeCallbacksAndMessages(null)
        listener.cancel()
        speaker.stop()

        statusView.text = "NOORA\n\nSilent"
        avatar.setState("Ready")

        if (wakeWordEnabled) {
            wakeWord.start(
                onWake = { commandAfterWake ->
                    wakeWord.stop()

                    runOnUiThread {
                        statusView.text = "NOORA\n\nAwake"
                        avatar.setState("Ready")

                        if (commandAfterWake.isBlank()) {
                            continuousConversation = true
                            voiceBusy = true
                            startListening(statusView)
                        } else if (handleSilentCommand(commandAfterWake, statusView)) {
                            return@runOnUiThread
                        } else {
                            continuousConversation = false
                            voiceBusy = true
                            statusView.text = "NOORA\n\nYou: $commandAfterWake\n\nThinking..."
                            avatar.setState("Thinking")
                            coordinator.handleTranscript(commandAfterWake)
                        }
                    }
                },
                onState = { _ ->
                    runOnUiThread {
                        statusView.text = "NOORA\n\nSay: Hey JARVIS"
                        avatar.setState("Ready")
                    }
                }
            )
        }

        return true
    }

    private fun scheduleListening(statusView: TextView, delayMs: Long) {
        if (!continuousConversation || voiceBusy || listeningRestartPending) {
            return
        }

        listeningRestartPending = true

        mainHandler.postDelayed({
            listeningRestartPending = false

            if (continuousConversation && !voiceBusy) {
                startListening(statusView)
            }
        }, delayMs)
    }

    private fun showBiometricPrompt(statusView: TextView) {
        val manager = BiometricManager.from(this)

        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                "Strong Android authentication is not available on this device.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    sessionAuthorization.authorizeOwner()
                    statusView.text = "NOORA\n\nStrong authentication successful."
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    statusView.text = "NOORA\n\nAuthentication cancelled or unavailable."
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("NOORA Security")
                .setSubtitle("Confirm that you are authorized to use NOORA")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }

    override fun onDestroy() {
        continuousConversation = false
        listeningRestartPending = false
        voiceBusy = false
        mainHandler.removeCallbacksAndMessages(null)
        coordinator.shutdown()
        listener.destroy()
        wakeWord.destroy()
        speaker.shutdown()
        super.onDestroy()
    }
}
