package com.togetherly.feature.today.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.togetherly.designsystem.theme.togetherlyColors
import com.togetherly.feature.today.model.QuestCategoryUi

/**
 * The one place [QuestCategoryUi] resolves to an actual [Color] — a Composable-only concern (needs
 * [MaterialTheme.togetherlyColors]), which is exactly why [QuestCategoryUi] itself stays a plain
 * enum with no [Color] field (see its own KDoc). Category color is always paired with the
 * category's text label elsewhere in the UI — never the only way a category is communicated (a
 * color-only signal fails for color-blind users).
 */
@Composable
@ReadOnlyComposable
fun QuestCategoryUi.color(): Color {
    val colors = MaterialTheme.togetherlyColors
    return when (this) {
        QuestCategoryUi.TALK -> colors.categoryTalk
        QuestCategoryUi.CREATE -> colors.categoryCreate
        QuestCategoryUi.MOVE -> colors.categoryMove
        QuestCategoryUi.KINDNESS -> colors.categoryKindness
        QuestCategoryUi.DISCOVER -> colors.categoryDiscover
        QuestCategoryUi.SILLY -> colors.categorySilly
        QuestCategoryUi.MEMORIES -> colors.categoryMemories
    }
}
