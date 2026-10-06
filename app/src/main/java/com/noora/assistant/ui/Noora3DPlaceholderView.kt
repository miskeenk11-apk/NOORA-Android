package com.noora.assistant.ui

import android.content.Context
import android.graphics.Color
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import io.github.sceneview.SceneView
import io.github.sceneview.model.rememberModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

/**
 * NOORA avatar surface.
 *
 * The existing state API is preserved so MainActivity and the voice/security
 * features do not need to change. The real VRM/GLB is loaded from:
 * assets/models/viverse_avatar_model_214366.glb
 *
 * If the binary asset is not bundled yet, the view stays a safe black surface
 * instead of changing any existing NOORA functionality.
 */
class Noora3DPlaceholderView(context: Context) : ComposeView(context) {
    private var currentState = "Ready"

    init {
        setBackgroundColor(Color.BLACK)
        setContent { NooraVrmScene(currentState) }
    }

    fun setState(value: String) {
        currentState = value
        setContent { NooraVrmScene(currentState) }
    }
}

@Composable
private fun NooraVrmScene(state: String) {
    val transition = rememberInfiniteTransition(label = "nooraVrmMotion")

    val bob by transition.animateFloat(
        initialValue = -0.012f,
        targetValue = 0.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state.equals("Speaking", true)) 420 else 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceBodyMotion"
    )

    val sway by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state.equals("Listening", true)) 1100 else 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleSway"
    )

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(
        modelLoader,
        "models/viverse_avatar_model_214366.glb"
    )

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(orbitRadius = 2.8f)
    ) {
        model?.let {
            ModelNode(
                modelInstance = it,
                autoAnimate = false,
                scaleToUnits = 1.65f,
                position = Position(y = bob),
                rotation = Rotation(y = sway)
            )
        }
    }
}
