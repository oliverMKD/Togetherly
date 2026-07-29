package com.togetherly.feature.questmode.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.questmode.model.QuestModeContentUi
import com.togetherly.feature.questmode.model.QuestTimerUi
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.questmode_phone_down_active_body
import togetherly.shared.generated.resources.questmode_phone_down_active_title

/**
 * A near-black, deliberately minimal, low-distraction surface — no instructions, no metadata, no
 * bottom actions except the implicit "tap anywhere to return." Never enables keep-screen-on itself
 * (that's Step 9.5's `KeepScreenOnEffect`, gated off entirely while phone-down is active — see that
 * effect's own future KDoc) and never renders a pulsing/looping animation, so reduced-motion needs
 * nothing special here — there is no motion to reduce in the first place.
 */
@Composable
internal fun QuestModePhoneDownScreen(
    quest: QuestModeContentUi,
    onAction: (QuestModeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exitDescription = stringResource(Res.string.questmode_phone_down_active_body)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onAction(QuestModeAction.ExitPhoneDownClicked) }
            .semantics { contentDescription = exitDescription },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            if (quest.timer is QuestTimerUi.Running) {
                Text(text = quest.timer.displayTime, style = MaterialTheme.togetherlyTypography.displayL, color = Color.White)
            }
            Text(
                text = stringResource(Res.string.questmode_phone_down_active_title),
                style = MaterialTheme.togetherlyTypography.titleM,
                color = Color.White,
            )
            Text(
                text = stringResource(Res.string.questmode_phone_down_active_body),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = Color.White,
            )
        }
    }
}
