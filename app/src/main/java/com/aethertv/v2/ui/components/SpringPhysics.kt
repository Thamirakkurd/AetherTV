package com.aethertv.v2.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.aethertv.v2.ui.theme.LiquidGlassTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class GridSpringPhysicsState(
    private val scope: CoroutineScope
) {
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
