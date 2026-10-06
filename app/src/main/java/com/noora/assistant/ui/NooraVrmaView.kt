package com.noora.assistant.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Real NOORA VRM surface. Android keeps the existing voice/AI logic;
 * this view only renders the avatar and receives visual states.
 */
class NooraVrmaView(context: Context) : WebView(context) {
    init {
        setBackgroundColor(Color.BLACK)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webViewClient = WebViewClient()
        webChromeClient = WebChromeClient()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        loadUrl("file:///android_asset/noora_vrm.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setState(value: String) {
        post {
            val safe = value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
            evaluateJavascript("window.nooraSetState && window.nooraSetState('$safe')", null)
        }
    }

    fun setExpression(name: String, value: Float) {
        post {
            val safe = name.replace("\\", "\\\\").replace("'", "\\'")
            evaluateJavascript("window.nooraExpression && window.nooraExpression('$safe',$value)", null)
        }
    }

    override fun onDetachedFromWindow() {
        stopLoading()
        loadUrl("about:blank")
        destroy()
        super.onDetachedFromWindow()
    }
}
