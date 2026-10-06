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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * MainActivity's existing state API is preserved. The VRM 0.x file is a GLB
 * container, so the packaged test asset is intentionally addressed as .glb.
 *
 * Expected asset:
 * assets/models/viverse_avatar_model_214366.glb
 */
class Noora3DPlaceholderView(context: Context) : ComposeView(context) {
    private var currentState by mutableStateOf("Ready")

    init {
        setBackgroundColor(Color.BLACK)
        setContent {
            NooraVrmScene(state = currentState)
        }
    }

    fun setState(value: String) {
        currentState = value
    }
}

@Composable
private fun NooraVrmScene(state: String) {
    val transition = rememberInfiniteTransition(label = "nooraVrmMotion")

    val speaking = state.equals("Speaking", true)
    val listening = state.equals("Listening", true)
    val salute = state.equals("Salute", true)

    val bob by transition.animateFloat(
        initialValue = -0.012f,
        targetValue = 0.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    speaking -> 420
                    listening -> 900
                    else -> 1500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceBodyMotion"
    )

    val sway by transition.animateFloat(
        initialValue = if (salute) -2.5f else -1.0f,
        targetValue = if (salute) 2.5f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    salute -> 650
                    listening -> 1100
                    speaking -> 850
                    else -> 1800
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleSway"
    )

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // SceneView 4.52.0 loads standard GLB/GLTF assets from src/main/assets.
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
        model?.let { instance ->
            ModelNode(
                modelInstance = instance,
                autoAnimate = false,
                scaleToUnits = 1.65f,
                position = Position(y = bob),
                rotation = Rotation(y = sway)
            )
        }
    }
}
