package com.togetherly.feature.packdetails.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.togetherly.core.ui.asString
import com.togetherly.designsystem.component.button.TogetherlyIconButton
import com.togetherly.designsystem.component.button.TogetherlyPrimaryButton
import com.togetherly.designsystem.component.feedback.TogetherlyInlineError
import com.togetherly.designsystem.component.feedback.TogetherlyLoadingIndicator
import com.togetherly.designsystem.component.layout.TogetherlyScreen
import com.togetherly.designsystem.component.navigation.TogetherlyTopBar
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.designsystem.theme.togetherlyShapes
import com.togetherly.designsystem.theme.togetherlySpacing
import com.togetherly.designsystem.theme.togetherlyTypography
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.QuestPackId
import com.togetherly.feature.explore.model.ExplorePackUiModel
import com.togetherly.feature.explore.model.ExploreQuestUiModel
import com.togetherly.feature.explore.presentation.ExploreQuestCard
import com.togetherly.feature.packdetails.model.ContentAccessState
import com.togetherly.feature.packdetails.model.PackDetailsUiState
import com.togetherly.feature.today.presentation.color
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import togetherly.shared.generated.resources.Res
import togetherly.shared.generated.resources.explore_pack_free
import togetherly.shared.generated.resources.explore_pack_premium
import togetherly.shared.generated.resources.explore_pack_quest_count
import togetherly.shared.generated.resources.packdetails_choose_quest_action
import togetherly.shared.generated.resources.packdetails_included_quests_title
import togetherly.shared.generated.resources.packdetails_locked_explanation
import togetherly.shared.generated.resources.packdetails_unlock_action
import togetherly.shared.generated.resources.ds_component_back_content_description

private val PACK_HERO_HEIGHT = 120.dp

@Composable
fun PackDetailsRoute(
    packId: QuestPackId,
    onNavigateBack: () -> Unit,
    onOpenQuestDetail: (QuestId) -> Unit,
    onOpenPaywall: (QuestPackId) -> Unit,
    viewModel: PackDetailsViewModel = koinViewModel { parametersOf(packId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onScreenStarted() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PackDetailsEvent.NavigateBack -> onNavigateBack()
                is PackDetailsEvent.OpenQuestDetail -> onOpenQuestDetail(event.questId)
                is PackDetailsEvent.OpenPaywall -> onOpenPaywall(event.packId)
            }
        }
    }

    PackDetailsScreen(state = state, onAction = viewModel::onAction, onNavigateBack = onNavigateBack)
}

@Composable
internal fun PackDetailsScreen(
    state: PackDetailsUiState,
    onAction: (PackDetailsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TogetherlyScreen(
        modifier = modifier,
        topBar = {
            TogetherlyTopBar(
                navigationIcon = {
                    TogetherlyIconButton(
                        icon = { Text("‹", style = MaterialTheme.togetherlyTypography.headlineM) },
                        contentDescription = stringResource(Res.string.ds_component_back_content_description),
                        onClick = { onAction(PackDetailsAction.BackClicked) },
                    )
                },
            )
        },
    ) {
        when {
            state.error != null -> TogetherlyInlineError(
                message = state.error.asString(),
                onRetry = { onAction(PackDetailsAction.RetryClicked) },
            )
            state.isLoading || state.pack == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TogetherlyLoadingIndicator()
            }
            else -> PackDetailsContent(pack = state.pack, quests = state.quests, accessState = state.accessState, onAction = onAction)
        }
    }
}

@Composable
private fun PackDetailsContent(
    pack: ExplorePackUiModel,
    quests: List<ExploreQuestUiModel>,
    accessState: ContentAccessState,
    onAction: (PackDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.togetherlyColors

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = MaterialTheme.togetherlySpacing.m),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.l),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PACK_HERO_HEIGHT)
                .background(pack.theme.color().copy(alpha = 0.16f), shape = MaterialTheme.togetherlyShapes.medium),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(MaterialTheme.togetherlySpacing.xxl)
                    .background(pack.theme.color(), shape = CircleShape),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs)) {
            Text(text = pack.title, style = MaterialTheme.togetherlyTypography.headlineM, color = colors.foregroundPrimary)
            Text(text = pack.description, style = MaterialTheme.togetherlyTypography.bodyL, color = colors.foregroundSecondary)

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (pack.isPremium) Res.string.explore_pack_premium else Res.string.explore_pack_free),
                    style = MaterialTheme.togetherlyTypography.labelL,
                    color = if (pack.isPremium) colors.actionPrimary else colors.foregroundSecondary,
                )
                Text(text = "•", style = MaterialTheme.togetherlyTypography.labelL, color = colors.foregroundSecondary)
                Text(
                    text = pluralStringResource(Res.plurals.explore_pack_quest_count, quests.size, quests.size),
                    style = MaterialTheme.togetherlyTypography.labelL,
                    color = colors.foregroundSecondary,
                )
                pack.durationLabel?.let { label ->
                    Text(text = "•", style = MaterialTheme.togetherlyTypography.labelL, color = colors.foregroundSecondary)
                    Text(text = label.asString(), style = MaterialTheme.togetherlyTypography.labelL, color = colors.foregroundSecondary)
                }
            }
        }

        if (accessState == ContentAccessState.LOCKED) {
            Text(
                text = stringResource(Res.string.packdetails_locked_explanation),
                style = MaterialTheme.togetherlyTypography.bodyM,
                color = colors.foregroundSecondary,
            )
        }

        if (accessState == ContentAccessState.LOCKED) {
            TogetherlyPrimaryButton(
                label = stringResource(Res.string.packdetails_unlock_action),
                onClick = { onAction(PackDetailsAction.UnlockClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TogetherlyPrimaryButton(
                label = stringResource(Res.string.packdetails_choose_quest_action),
                onClick = { quests.firstOrNull()?.let { onAction(PackDetailsAction.QuestClicked(it.id)) } },
                enabled = quests.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.togetherlySpacing.s)) {
            Text(
                text = stringResource(Res.string.packdetails_included_quests_title),
                style = MaterialTheme.togetherlyTypography.titleL,
                color = colors.foregroundPrimary,
            )
            quests.forEach { quest ->
                ExploreQuestCard(
                    quest = quest,
                    onClick = { onAction(PackDetailsAction.QuestClicked(quest.id)) },
                    onSaveClick = { onAction(PackDetailsAction.SaveClicked(quest.id)) },
                )
            }
        }
    }
}
