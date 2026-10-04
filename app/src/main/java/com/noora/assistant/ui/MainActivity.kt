package com.noora.assistant.ui

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.noora.assistant.R
import com.noora.assistant.ai.GatewayAiClient
import com.noora.assistant.ai.NooraConversationCoordinator
import com.noora.assistant.auth.NooraLocalAccountStore
import com.noora.assistant.core.NooraConnectivity
import com.noora.assistant.memory.NooraMemoryStore
import com.noora.assistant.phone.NooraPhoneControl
import com.noora.assistant.voice.NooraSpeechRecognizer
import com.noora.assistant.voice.NooraTextToSpeech
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var account: NooraLocalAccountStore
    private lateinit var avatar: NooraAvatarView
    private lateinit var speaker: NooraTextToSpeech
    private lateinit var listener: NooraSpeechRecognizer
    private lateinit var coordinator: NooraConversationCoordinator
    private val handler=Handler(Looper.getMainLooper())
    private var listening=false
    private var authScreen: LinearLayout?=null
    private var gatewayUrl=""

    private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if(::listener.isInitialized) startListening()
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        account=NooraLocalAccountStore(this)
        if(account.hasAccount() && account.isVerified()) showMain() else showAuth()
    }

    private fun showAuth() {
        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER
            setPadding(44,32,44,32)
            setBackgroundColor(0xFF05070B.toInt())
        }
        val title=TextView(this).apply{ text="NOORA"; textSize=38f; setTextColor(0xFFBFE7FF.toInt()); gravity=Gravity.CENTER }
        val sub=TextView(this).apply{ text="AI Voice Assistant"; textSize=16f; setTextColor(0xFFE8F5FF.toInt()); gravity=Gravity.CENTER; setPadding(0,8,0,28) }
        val email=EditText(this).apply{ hint="Email"; setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF8A98A8.toInt()); inputType=33 }
        val password=EditText(this).apply{ hint="Password (6+ characters)"; setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF8A98A8.toInt()); inputType=129 }
        val login=Button(this).apply{ text="LOGIN" }
        val signup=Button(this).apply{ text="SIGN UP" }
        val info=TextView(this).apply{ text="Your account is stored locally on this device. Login is followed by Android device verification."; setTextColor(0xFF9FB0C0.toInt()); textSize=13f; setPadding(0,22,0,0) }
        root.addView(title);root.addView(sub);root.addView(email,LinearLayout.LayoutParams(-1,60));root.addView(password,LinearLayout.LayoutParams(-1,60))
        root.addView(login,LinearLayout.LayoutParams(-1,58));root.addView(signup,LinearLayout.LayoutParams(-1,58));root.addView(info)
        login.setOnClickListener{
            if(account.login(email.text.toString(),password.text.toString())) verifyDevice()
            else Toast.makeText(this,"Login failed.",Toast.LENGTH_SHORT).show()
        }
        signup.setOnClickListener{
            if(account.signUp(email.text.toString(),password.text.toString())) {
                Toast.makeText(this,"Account created. Please verify this device.",Toast.LENGTH_LONG).show()
                verifyDevice()
            } else Toast.makeText(this,"Use a valid email and password of at least 6 characters.",Toast.LENGTH_LONG).show()
        }
        authScreen=root
        setContentView(root)
    }

    private fun verifyDevice() {
        val manager=BiometricManager.from(this)
        val can=manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if(can==BiometricManager.BIOMETRIC_SUCCESS) {
            val executor=ContextCompat.getMainExecutor(this)
            val prompt=BiometricPrompt(this,executor,object:BiometricPrompt.AuthenticationCallback(){
                override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){
                    account.verifyDevice(); showMain()
                }
                override fun onAuthenticationError(code:Int,msg:CharSequence){ Toast.makeText(this@MainActivity,"Verification cancelled: $msg",Toast.LENGTH_SHORT).show() }
            })
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verify NOORA")
                    .setSubtitle("Confirm that this is your device session")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
            )
        } else {
            account.verifyDevice()
            showMain()
        }
    }

    private fun showMain() {
        gatewayUrl=getSharedPreferences("noora_settings",Context.MODE_PRIVATE).getString("gateway_url","").orEmpty()
        speaker=NooraTextToSpeech(this)
        listener=NooraSpeechRecognizer(this)
        avatar=NooraAvatarView(this){ action->
            when(action){
                NooraAvatarView.Action.VOICE -> startListening()
                NooraAvatarView.Action.NOORA -> { avatar.setState("Ready"); startListening() }
                NooraAvatarView.Action.SETTINGS -> showMenu()
                NooraAvatarView.Action.EXIT -> finishAndRemoveTask()
            }
        }
        avatar.setState("Ready")

        coordinator=NooraConversationCoordinator(
            NooraConnectivity(this),
            NooraMemoryStore(this),
            speaker,
            GatewayAiClient(gatewayUrl),
            NooraPhoneControl(this)
        ){ msg->
            runOnUiThread{
                avatar.setState(if(msg.contains("Thinking",true))"Thinking" else if(msg.contains("Speaking",true))"Speaking" else "Ready")
            }
        }
        speaker.setSpeakingListener{active->runOnUiThread{avatar.setState(if(active)"Speaking" else "Ready")}}
        speaker.setCompletionListener{
            runOnUiThread{
                avatar.setState("Ready")
                if(listening) handler.postDelayed({startListening()},450)
            }
        }

        val root=FrameLayout(this).apply{ setBackgroundColor(0xFF000000.toInt()); addView(avatar,FrameLayout.LayoutParams(-1,-1)) }
        val menu=Button(this).apply{ text="⋮"; textSize=28f; setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0x00000000); setOnClickListener{showMenu()} }
        root.addView(menu,FrameLayout.LayoutParams(72,72).apply{gravity=Gravity.TOP or Gravity.END;topMargin=10;marginEnd=8})
        setContentView(root)

        permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        if(!account.firstGreetingDone()){
            avatar.setState("Salute")
            handler.postDelayed({
                speaker.speak(greeting(),"en")
                account.markFirstGreetingDone()
            },650)
        } else {
            handler.postDelayed({startListening()},700)
        }
    }

    private fun greeting():String {
        val h=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val time=when(h){in 5..11->"Good morning.";in 12..16->"Good afternoon.";in 17..20->"Good evening.";else->"Good night."}
        return "Assalam-o-Alaikum. $time Ji, aap kya pasand karenge?"
    }

    private fun startListening() {
        if(!::listener.isInitialized || listening || !listener.isAvailable()) return
        listening=true
        avatar.setState("Listening")
        listener.listen(
            languageTag="en-US",
            onText={text->
                listening=false
                runOnUiThread{
                    avatar.setState("Thinking")
                    coordinator.handleTranscript(text)
                }
            },
            onError={
                listening=false
                runOnUiThread{avatar.setState("Ready")}
            },
            onState={state->
                runOnUiThread{
                    if(state.contains("Listening",true)) avatar.setState("Listening")
                    else if(state.contains("Processing",true)) avatar.setState("Thinking")
                }
            }
        )
    }

    private fun showMenu() {
        val popup=PopupMenu(this,avatar)
        popup.menu.add("NOORA")
        popup.menu.add("VOICE / ASK")
        popup.menu.add("SETTINGS")
        popup.menu.add("LOG OUT")
        popup.setOnMenuItemClickListener{
            when(it.title.toString()){
                "NOORA"->avatar.setState("Ready")
                "VOICE / ASK"->startListening()
                "SETTINGS"->showSettings()
                "LOG OUT"->{ account.logout(); listener.cancel(); speaker.stop(); showAuth() }
            }
            true
        }
        popup.show()
    }

    private fun showSettings() {
        val input=EditText(this).apply{hint="https://your-noora-gateway.example.com";setText(gatewayUrl)}
        AlertDialogBuilder(this,"NOORA AI Gateway",input){
            gatewayUrl=input.text.toString().trim()
            getSharedPreferences("noora_settings",Context.MODE_PRIVATE).edit().putString("gateway_url",gatewayUrl).apply()
            Toast.makeText(this,"Gateway setting saved.",Toast.LENGTH_SHORT).show()
        }
    }

    private fun AlertDialogBuilder(context:Context,title:String,input:EditText,onSave:()->Unit) {
        android.app.AlertDialog.Builder(context).setTitle(title).setView(input)
            .setPositiveButton("SAVE"){_,_->onSave()}.setNegativeButton("CANCEL",null).show()
    }

    override fun onDestroy() {
        if(::listener.isInitialized) listener.destroy()
        if(::speaker.isInitialized) speaker.shutdown()
        if(::coordinator.isInitialized) coordinator.shutdown()
        super.onDestroy()
    }
}
