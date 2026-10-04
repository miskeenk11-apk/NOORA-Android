package com.noora.assistant.ui

import android.content.Context
import android.graphics.Color
import android.view.View
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.utils.colorOf
import com.google.android.filament.LightManager

/**
 * Temporary 3D NOORA implementation.
 * This is deliberately procedural so the 3D pipeline can be tested before the
 * final rigged GLB arrives. The same state API will drive the final character.
 */
class Noora3DPlaceholderView(context: Context) : ComposeView(context) {
    private var currentState by mutableStateOf("Ready")

    init {
        setBackgroundColor(Color.BLACK)
        setContent {
            Noora3DPlaceholderScene(currentState)
        }
    }

    fun setState(value: String) {
        currentState = value
    }
}

@Composable
private fun Noora3DPlaceholderScene(state: String) {
    val transition = rememberInfiniteTransition(label = "noora3d")
    val bob by transition.animateFloat(
        initialValue = -0.018f,
        targetValue = 0.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state.equals("Speaking", true)) 520 else 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bodyBob"
    )

    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    val engine = io.github.sceneview.rememberEngine()
    val materialLoader = io.github.sceneview.rememberMaterialLoader(engine)

    val skin = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(ComposeColor(0.82f, 0.62f, 0.48f, 1f)))
    }
    val dress = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(ComposeColor(0.12f, 0.32f, 0.82f, 1f)))
    }
    val hijab = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(ComposeColor(0.08f, 0.20f, 0.58f, 1f)))
    }
    val white = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(ComposeColor.White))
    }
    val eye = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(ComposeColor(0.08f, 0.28f, 0.95f, 1f)))
    }
    val mouth = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(ComposeColor(0.18f, 0.02f, 0.04f, 1f)))
    }

    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = io.github.sceneview.rememberModelLoader(engine),
        cameraManipulator = io.github.sceneview.rememberCameraManipulator(
            orbitRadius = 3.1f
        )
    ) {
        LightNode(
            type = LightManager.Type.SUN,
            apply = {
                intensity(100_000f)
                castShadows(true)
            }
        )

        // Body / dress
        CylinderNode(
            radius = 0.48f,
            height = 1.35f,
            materialInstance = dress,
            position = Position(y = -0.55f + bob)
        )

        // Head + hijab
        SphereNode(
            radius = 0.42f,
            materialInstance = skin,
            position = Position(y = 0.42f + bob)
        )
        SphereNode(
            radius = 0.50f,
            materialInstance = hijab,
            position = Position(y = 0.48f + bob, z = 0.02f)
            scale = Scale(1.0f, 1.05f, 0.90f)
        )

        // Eyes
        SphereNode(
            radius = 0.055f,
            materialInstance = eye,
            position = Position(x = -0.16f, y = 0.47f + bob, z = -0.385f),
            scale = Scale(1f, blink, 0.35f)
        )
        SphereNode(
            radius = 0.055f,
            materialInstance = eye,
            position = Position(x = 0.16f, y = 0.47f + bob, z = -0.385f),
            scale = Scale(1f, blink, 0.35f)
        )

        // Mouth opens during speaking.
        SphereNode(
            radius = if (state.equals("Speaking", true)) 0.075f else 0.035f,
            materialInstance = mouth,
            position = Position(y = 0.25f + bob, z = -0.395f),
            scale = Scale(1.25f, if (state.equals("Speaking", true)) 1.5f else 0.45f, 0.35f)
        )

        // Arms
        CylinderNode(
            radius = 0.09f,
            height = 0.95f,
            materialInstance = dress,
            position = Position(x = -0.58f, y = -0.48f + bob),
            rotation = Rotation(z = 0.20f)
        )
        CylinderNode(
            radius = 0.09f,
            height = 0.95f,
            materialInstance = dress,
            position = Position(x = 0.58f, y = -0.48f + bob),
            rotation = Rotation(z = -0.20f)
        )

        // Hands
        SphereNode(
            radius = 0.12f,
            materialInstance = skin,
            position = Position(x = -0.67f, y = -0.98f + bob)
        )
        SphereNode(
            radius = 0.12f,
            materialInstance = skin,
            position = Position(x = 0.67f, y = -0.98f + bob)
        )

        // Simple base/feet.
        CubeNode(
            size = Size(1.55f, 0.10f, 0.70f),
            materialInstance = hijab,
            position = Position(y = -1.28f)
        )
    }
}
