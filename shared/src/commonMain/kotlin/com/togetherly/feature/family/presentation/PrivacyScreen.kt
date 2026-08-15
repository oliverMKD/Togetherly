package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.PrivacyAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.privacy_intro
import togetherly.shared.generated.resources.privacy_memories_body
import togetherly.shared.generated.resources.privacy_memories_title
import togetherly.shared.generated.resources.privacy_no_account_body
import togetherly.shared.generated.resources.privacy_no_account_title
import togetherly.shared.generated.resources.privacy_notifications_body
import togetherly.shared.generated.resources.privacy_notifications_title
import togetherly.shared.generated.resources.privacy_profile_body
import togetherly.shared.generated.resources.privacy_profile_title
import togetherly.shared.generated.resources.privacy_progress_body
import togetherly.shared.generated.resources.privacy_progress_title
import togetherly.shared.generated.resources.privacy_purchases_body
import togetherly.shared.generated.resources.privacy_purchases_title
import togetherly.shared.generated.resources.privacy_restore_purchases_body
import togetherly.shared.generated.resources.privacy_restore_purchases_title
import togetherly.shared.generated.resources.privacy_title
import togetherly.shared.generated.resources.privacy_uninstall_body
import togetherly.shared.generated.resources.privacy_uninstall_title
import togetherly.shared.generated.resources.ds_component_back_content_description

private data class PrivacySection(val title: StringResource, val body: StringResource)

private val PRIVACY_SECTIONS = listOf(
    PrivacySection(Res.string.privacy_profile_title, Res.string.privacy_profile_body),
    PrivacySection(Res.string.privacy_progress_title, Res.string.privacy_progress_body),
    PrivacySection(Res.string.privacy_memories_title, Res.string.privacy_memories_body),
    PrivacySection(Res.string.privacy_purchases_title, Res.string.privacy_purchases_body),
    PrivacySection(Res.string.privacy_notifications_title, Res.string.privacy_notifications_body),
    PrivacySection(Res.string.privacy_no_account_title, Res.string.privacy_no_account_body),
    PrivacySection(Res.string.privacy_uninstall_title, Res.string.privacy_uninstall_body),
    PrivacySection(Res.string.privacy_restore_purchases_title, Res.string.privacy_restore_purchases_body),
)

@Composable
internal fun PrivacyScreen(
    onAction: (PrivacyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.privacy_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(PrivacyAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
        ) {
            Text(
                text = stringResource(Res.string.privacy_intro),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            PRIVACY_SECTIONS.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
                    Text(
                        text = stringResource(section.title),
                        style = MaterialTheme.togetherlyTypography.titleM,
                        color = MaterialTheme.togetherlyColors.foregroundPrimary,
                    )
                    Text(
                        text = stringResource(section.body),
                        style = MaterialTheme.togetherlyTypography.bodyM,
                        color = MaterialTheme.togetherlyColors.foregroundSecondary,
                    )
                }
            }
        }
    }
}
