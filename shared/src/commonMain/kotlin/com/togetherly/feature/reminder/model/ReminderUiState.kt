package com.togetherly.feature.reminder.model

import androidx.compose.runtime.Immutable
import com.togetherly.core.notification.NotificationPermissionState
import com.togetherly.core.ui.UiText
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * Deliberately keeps three separate, independently-observable facts distinct rather than
 * collapsing them into one boolean, per this step's own requirement: [enabled] ("reminder
 * preference enabled" — the family's own stored intent), [permissionState] ("notification
 * permission granted" — what the OS actually allows), and whether a reminder is *truly* active
 * right now is never stored as its own field — it's simply `enabled && permissionState` being
 * go-ahead, computed at the UI layer (see `ReminderScreen`'s own inline warning), never persisted
 * as a fourth redundant flag.
 */
@Immutable
data class ReminderUiState(
    val isLoading: Boolean = true,
    val enabled: Boolean = false,
    val days: PersistentSet<DayOfWeek> = persistentSetOf(),
    val time: LocalTime? = null,
    val permissionState: NotificationPermissionState = NotificationPermissionState.NotDetermined,
    val validationErrors: PersistentMap<ReminderField, UiText> = persistentMapOf(),
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val error: UiText? = null,
)
