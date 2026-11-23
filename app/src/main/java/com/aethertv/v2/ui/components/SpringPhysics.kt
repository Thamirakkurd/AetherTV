package com.aethertv.v2.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class GridSpringPhysicsState(
    private val scope: CoroutineScope
) {
    // Synchronous primitive float states — zero coroutine/GC allocations per scroll frame
    var verticalImpulse by mutableFloatStateOf(0f)
        private set
    var lateralImpulse by mutableFloatStateOf(0f)
        private set

    private var settleJob: Job? = null

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            settleJob?.cancel()
            val targetY = (available.y / 70f).coerceIn(-1f, 1f)
            val targetX = (available.x / 70f).coerceIn(-1f, 1f)
            verticalImpulse = targetY * 0.50f + verticalImpulse * 0.50f
            if (abs(targetX) > 0.02f) {
                lateralImpulse = targetX * 0.45f + lateralImpulse * 0.55f
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    fun settle() {
        settleJob?.cancel()
        val startV = verticalImpulse
        val startH = lateralImpulse
        if (abs(startV) < 0.001f && abs(startH) < 0.001f) {
            verticalImpulse = 0f
            lateralImpulse = 0f
            return
        }
        settleJob = scope.launch {
            launch {
                AnimationState(initialValue = startV).animateTo(
                    targetValue = 0f,
                    animationSpec = LiquidGlassTokens.meshSettleSpring()
                ) {
                    verticalImpulse = value
                }
            }
            launch {
                AnimationState(initialValue = startH).animateTo(
                    targetValue = 0f,
                    animationSpec = LiquidGlassTokens.meshSettleSpring()
                ) {
                    lateralImpulse = value
                }
            }
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
            isPressed -> 1.045f
            isFocused || isSelected -> 1.06f
            else -> 1f
        },
        animationSpec = LiquidGlassTokens.popSpring(),
        label = "cardPopScale"
    )

    val popElevationLift by animateFloatAsState(
        targetValue = if (active) -7f else 0f,
        animationSpec = LiquidGlassTokens.popSpring(),
        label = "cardLift"
    )

    val col = index % columns.coerceAtLeast(1)
    val staggerFactor = 1f + (col * 0.12f)

    this
        .zIndex(if (active) 10f else 0f)
        .graphicsLayer {
            scaleX = popScale
            scaleY = popScale
            translationY = popElevationLift + (physicsState.verticalImpulse * 11f * staggerFactor)
            translationX = physicsState.lateralImpulse * 8f * staggerFactor
            shadowElevation = if (active) 20f else 0f
        }
}
