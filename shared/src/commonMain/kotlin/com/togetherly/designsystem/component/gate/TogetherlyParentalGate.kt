package com.togetherly.designsystem.component.gate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.togetherly.designsystem.component.button.TogetherlyTextButton
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.ds_component_dismiss
import togetherly.shared.generated.resources.ds_parental_gate_body
import togetherly.shared.generated.resources.ds_parental_gate_hold_content_description
import togetherly.shared.generated.resources.ds_parental_gate_title
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val HOLD_DURATION = 3.seconds
private const val TICK_MILLIS = 16L

/**
 * A lightweight speed bump before a purchase-capable paywall opens from a child-facing experience
 * (Today's reroll limit, a locked premium quest) — never a real identity check, never collecting
 * any child personal data, and never RevenueCat-aware (see this file's own placement in
 * `designsystem`, not `data.purchase`/`domain.purchase` — [onConfirmed] is a plain callback the
 * caller decides what to do with, most often "now actually navigate to the paywall").
 *
 * A touch user must press and hold [holdDuration] (default 3s); releasing early cancels with no
 * effect, same as tapping outside or [onDismiss]'s own Cancel action — no frightening or punitive
 * feedback either way, since accidentally releasing early is not a meaningful "failure." A screen
 * reader user instead gets a standard double-tap [Role.Button] action via [onClick] semantics —
 * holding a touch for a fixed duration isn't a reliable interaction for assistive tech, so the
 * already-deliberate act of exploring to this control and double-tapping it is treated as
 * sufficient intent for that access pattern, without weakening the touch-based safeguard for
 * everyone else. This dialog is shown once per attempt — the caller (e.g.
 * [com.togetherly.navigation.host.TogetherlyNavHost]) only asks for it right before opening the
 * paywall, never again during the purchase flow that follows.
 */
@Composable
fun TogetherlyParentalGateDialog(
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    holdDuration: Duration = HOLD_DURATION,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(Res.string.ds_parental_gate_title), style = MaterialTheme.togetherlyTypography.titleL) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m)) {
                Text(stringResource(Res.string.ds_parental_gate_body), style = MaterialTheme.togetherlyTypography.bodyM)
                HoldToConfirmButton(onConfirmed = onConfirmed, holdDuration = holdDuration)
            }
        },
        confirmButton = {},
        dismissButton = {
            TogetherlyTextButton(label = stringResource(Res.string.ds_component_dismiss), onClick = onDismiss)
        },
    )
}

@Composable
private fun HoldToConfirmButton(
    onConfirmed: () -> Unit,
    holdDuration: Duration,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val description = stringResource(Res.string.ds_parental_gate_hold_content_description)

    fun cancelHold() {
        holdJob?.cancel()
        holdJob = null
        progress = 0f
    }

    fun startHold() {
        if (holdJob != null) return
        holdJob = scope.launch {
            val totalMillis = holdDuration.inWholeMilliseconds
            var elapsed = 0L
            while (elapsed < totalMillis) {
                delay(TICK_MILLIS)
                elapsed += TICK_MILLIS
                progress = (elapsed.toFloat() / totalMillis).coerceAtMost(1f)
            }
            holdJob = null
            progress = 0f
            onConfirmed()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .size(64.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown()
                        startHold()
                        waitForUpOrCancellation()
                        cancelHold()
                    }
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick {
                    onConfirmed()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val trackColor = MaterialTheme.togetherlyColors.borderSubtle
        val progressColor = MaterialTheme.togetherlyColors.actionPrimary
        Canvas(modifier = Modifier.size(56.dp)) {
            val strokeWidth = 6.dp.toPx()
            drawCircle(color = trackColor, style = Stroke(width = strokeWidth))
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }
}
