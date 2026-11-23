package com.aethertv.v2.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import com.aethertv.v2.ui.theme.LiquidGlassTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class GridSpringPhysicsState(
    private val scope: CoroutineScope
) {
    val verticalImpulse = Animatable(0f)
    val lateralImpulse = Animatable(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val targetY = (available.y / 65f).coerceIn(-1f, 1f)
            val targetX = (available.x / 65f).coerceIn(-1f, 1f)
            scope.launch {
                verticalImpulse.snapTo(targetY * 0.55f + verticalImpulse.value * 0.45f)
                if (abs(targetX) > 0.02f) {
                    lateralImpulse.snapTo(targetX * 0.5f + lateralImpulse.value * 0.5f)
                }
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            scope.launch {
                verticalImpulse.animateTo(0f, LiquidGlassTokens.meshSettleSpring())
            }
            scope.launch {
                lateralImpulse.animateTo(0f, LiquidGlassTokens.meshSettleSpring())
            }
            return Velocity.Zero
        }
    }

    fun settle() {
        scope.launch {
            verticalImpulse.animateTo(0f, LiquidGlassTokens.meshSettleSpring())
            lateralImpulse.animateTo(0f, LiquidGlassTokens.meshSettleSpring())
        }
    }
}

@Composable
fun rememberGridSpringPhysicsState(): GridSpringPhysicsState {
    val scope = rememberCoroutineScope()
    return remember(scope) { GridSpringPhysicsState(scope) }
}

fun Modifier.connectedCardPhysics(
    index: Int,
    columns: Int,
    physicsState: GridSpringPhysicsState,
    interactionSource: MutableInteractionSource,
    isSelected: Boolean = false
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val active = isPressed || isFocused || isSelected

    val popScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 1.055f
            isFocused || isSelected -> 1.07f
            else -> 1f
        },
        animationSpec = LiquidGlassTokens.popSpring(),
        label = "cardPopScale"
    )

    val popElevationLift by animateFloatAsState(
        targetValue = if (active) -8f else 0f,
        animationSpec = LiquidGlassTokens.popSpring(),
        label = "cardLift"
    )

    val col = index % columns.coerceAtLeast(1)
    val staggerFactor = 1f + (col * 0.14f)

    this
        .zIndex(if (active) 10f else 0f)
        .graphicsLayer {
            val vImpulse = physicsState.verticalImpulse.value
            val hImpulse = physicsState.lateralImpulse.value

            scaleX = popScale
            scaleY = popScale
            translationY = popElevationLift + (vImpulse * 14f * staggerFactor)
            translationX = hImpulse * 10f * staggerFactor
            cameraDistance = 16f * density
            rotationX = (vImpulse * 3.2f).coerceIn(-4f, 4f)
            rotationY = (-hImpulse * 3.0f).coerceIn(-4f, 4f)
            shadowElevation = if (active) 24f else 0f
        }
}
