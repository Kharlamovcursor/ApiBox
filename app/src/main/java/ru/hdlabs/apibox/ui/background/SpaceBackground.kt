package ru.hdlabs.apibox.ui.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import ru.hdlabs.apibox.ApiBoxApp
import ru.hdlabs.apibox.data.AppSettings
import ru.hdlabs.apibox.ui.theme.HdBlack
import ru.hdlabs.apibox.ui.theme.HdBlue
import ru.hdlabs.apibox.ui.theme.HdOrange
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val dirX: Float,
    val dirY: Float,
    val depthOffset: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float,
)

private data class ShootingStar(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val offset: Float,
    val speed: Float,
    val trail: Float,
)

private data class DistantPulsar(
    val x: Float,
    val y: Float,
    val tint: Color,
    val speed: Float,
    val offset: Float,
)

@Composable
fun SpaceBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ApiBoxApp
    val settings by app.container.settings.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings(),
    )
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reduceMotion = remember {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
    val animEnabled = settings.animationEnabled &&
        !reduceMotion &&
        lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val speedFactor = (settings.animationSpeedPercent / 100f).coerceAtLeast(0.25f)
    val powerSave = settings.powerSave
    val starCount = if (powerSave) 160 else 380
    val pulsarCount = if (powerSave) 3 else 6
    val meteorCount = if (powerSave) 2 else 4

    val stars = remember(starCount) {
        val random = Random(42)
        List(starCount) {
            var dirX = random.nextFloat() * 2f - 1f
            var dirY = random.nextFloat() * 2f - 1f
            if (dirX * dirX + dirY * dirY < 0.06f) {
                dirX = if (dirX >= 0f) 0.4f else -0.4f
                dirY = if (dirY >= 0f) 0.4f else -0.4f
            }
            val bright = random.nextFloat() > 0.82f
            Star(
                dirX = dirX,
                dirY = dirY,
                depthOffset = random.nextFloat(),
                speed = if (random.nextFloat() > 0.55f) 1f else 2f,
                radius = if (bright) random.nextFloat() * 0.7f + 1.15f else random.nextFloat() * 0.7f + 0.4f,
                alpha = if (bright) random.nextFloat() * 0.12f + 0.88f else random.nextFloat() * 0.25f + 0.58f,
                twinkleSpeed = (random.nextInt(3) + 1).toFloat(),
                twinkleOffset = random.nextFloat(),
            )
        }
    }
    val meteors = remember(meteorCount) {
        val random = Random(11)
        List(meteorCount) {
            val leftward = random.nextBoolean()
            ShootingStar(
                startX = if (leftward) random.nextFloat() * 0.35f + 0.55f else random.nextFloat() * 0.35f + 0.05f,
                startY = random.nextFloat() * 0.25f,
                vx = if (leftward) -(random.nextFloat() * 0.22f + 0.28f) else random.nextFloat() * 0.22f + 0.28f,
                vy = random.nextFloat() * 0.18f + 0.34f,
                offset = random.nextFloat(),
                speed = 1f,
                trail = random.nextFloat() * 0.05f + 0.07f,
            )
        }
    }
    val pulsars = remember(pulsarCount) {
        val random = Random(7)
        List(pulsarCount) { index ->
            DistantPulsar(
                x = random.nextFloat() * 0.86f + 0.07f,
                y = random.nextFloat() * 0.72f + 0.06f,
                tint = if (index % 2 == 0) HdBlue else HdOrange,
                speed = if (index % 2 == 0) 3f else 2f,
                offset = random.nextFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "space")
    val phase = if (animEnabled) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (28_000 / speedFactor).toInt().coerceAtLeast(1),
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "phase",
        ).value
    } else {
        0f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(HdBlack)
        drawRect(Color(0xFF070B16))
        val vanishX = size.width * 0.5f
        val vanishY = size.height * 0.46f
        stars.forEach { star ->
            val travel = if (animEnabled) {
                ((phase * star.speed + star.depthOffset) % 1f + 1f) % 1f
            } else {
                0.28f + star.depthOffset * 0.45f
            }
            val depth = 0.06f + travel * travel * 0.94f
            val life = if (animEnabled) {
                val fadeIn = (travel / 0.12f).coerceIn(0f, 1f)
                val fadeOut = ((1f - travel) / 0.14f).coerceIn(0f, 1f)
                fadeIn * fadeOut
            } else {
                1f
            }
            val twinkle = if (animEnabled) {
                val wave = sin((phase * star.twinkleSpeed + star.twinkleOffset) * 6.28318f)
                (0.62f + 0.38f * wave).coerceIn(0.35f, 1f)
            } else {
                1f
            }
            drawCircle(
                color = Color.White.copy(alpha = (star.alpha * (0.35f + depth * 0.65f) * twinkle * life).coerceIn(0f, 1f)),
                radius = star.radius * (0.35f + depth * 1.35f),
                center = Offset(
                    vanishX + star.dirX * size.width * 0.62f * depth,
                    vanishY + star.dirY * size.height * 0.62f * depth,
                ),
            )
        }
        if (animEnabled) {
            meteors.forEach { meteor ->
                val local = ((phase * meteor.speed + meteor.offset) % 1f + 1f) % 1f
                if (local < 0.16f) {
                    val progress = local / 0.16f
                    val head = Offset(
                        (meteor.startX + meteor.vx * progress) * size.width,
                        (meteor.startY + meteor.vy * progress) * size.height,
                    )
                    val tail = Offset(
                        head.x - meteor.vx * meteor.trail * size.width,
                        head.y - meteor.vy * meteor.trail * size.height,
                    )
                    val fade = if (progress < 0.15f) progress / 0.15f else (1f - progress)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.88f * fade)),
                            start = tail,
                            end = head,
                        ),
                        start = tail,
                        end = head,
                        strokeWidth = 1.6f,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = fade.coerceIn(0f, 1f)),
                        radius = 2.4f,
                        center = head,
                    )
                }
            }
        }
        pulsars.forEach { pulsar ->
            val center = Offset(pulsar.x * size.width, pulsar.y * size.height)
            val wave = if (animEnabled) sin((phase * pulsar.speed + pulsar.offset) * 6.28318f) else 0f
            val raised = (0.5f + 0.5f * wave).coerceIn(0f, 1f)
            val flash = raised * raised * raised
            drawCircle(color = pulsar.tint.copy(alpha = 0.34f * flash), radius = 4f + 8f * flash, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.12f + 0.88f * flash),
                radius = 1.1f + 0.9f * flash,
                center = center,
            )
            if (animEnabled && !powerSave) {
                val ring = ((phase * pulsar.speed + pulsar.offset) % 1f + 1f) % 1f
                if (ring < 0.28f) {
                    val progress = ring / 0.28f
                    val burst = if (progress < 0.18f) progress / 0.18f else 1f - (progress - 0.18f) / 0.82f
                    drawCircle(
                        color = pulsar.tint.copy(alpha = burst.coerceIn(0f, 1f) * 0.7f),
                        radius = 2.5f + progress * 16f,
                        center = center,
                        style = Stroke(width = 1.3f),
                    )
                }
            }
        }
    }
}
