package com.togetherly.feature.family.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.card.TogetherlyCard
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.feature.family.model.OPEN_SOURCE_LICENSES
import com.togetherly.feature.family.model.OpenSourceLicensesAction
import org.jetbrains.compose.resources.stringResource
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.open_source_licenses_intro
import togetherly.shared.generated.resources.open_source_licenses_title

@Composable
internal fun OpenSourceLicensesScreen(
    onAction: (OpenSourceLicensesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                title = stringResource(Res.string.open_source_licenses_title),
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = "Back",
                        onClick = { onAction(OpenSourceLicensesAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.m),
        ) {
            Text(
                text = stringResource(Res.string.open_source_licenses_intro),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = MaterialTheme.togetherlyColors.foregroundSecondary,
            )
            OPEN_SOURCE_LICENSES.forEach { license ->
                TogetherlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction(OpenSourceLicensesAction.LicenseClicked(license)) },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.togetherlySpacing.m),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
                    ) {
                        Text(
                            text = license.name,
                            style = MaterialTheme.togetherlyTypography.titleM,
                            color = MaterialTheme.togetherlyColors.foregroundPrimary,
                        )
                        Text(
                            text = license.licenseType,
                            style = MaterialTheme.togetherlyTypography.bodyS,
                            color = MaterialTheme.togetherlyColors.foregroundSecondary,
                        )
                    }
                }
            }
        }
    }
}
