package com.togetherly.core.notification

import com.togetherly.core.result.DataResult
import com.togetherly.domain.family.ReminderPreference

/**
 * The one platform boundary for turning a family's [ReminderPreference] into an actual scheduled
 * local notification — resolved via Koin (`platformModule`, same convention as
 * [com.togetherly.core.feedback.QuestFeedbackController]), never `expect`/`actual` on the
 * interface itself (no Compose lifecycle involved here, unlike [NotificationPermissionController]).
 *
 * Deliberately takes [ReminderPreference] directly rather than a separate `ReminderSchedule` type
 * with its own `enabled: Boolean` — [ReminderPreference] is already all-or-nothing by construction
 * (non-null *is* "enabled"; see [com.togetherly.domain.family.FamilySettings]'s own KDoc for why a
 * parallel `enabled` flag next to a nullable time/day set would only represent a state that can't
 * happen). [cancel] is the caller's job when preferences become disabled (`null`), not something
 * [schedule] infers from an "enabled" field that doesn't exist here.
 *
 * Every method returns [DataResult] with the existing [com.togetherly.core.error.AppError.Permission]/
 * [com.togetherly.core.error.AppError.Unexpected] categories rather than a new parallel result
 * type — this project already has a typed-error convention everywhere else a platform call can
 * fail; inventing a second one here would just be the same shape with a different name.
 */
interface ReminderScheduler {

    suspend fun schedule(preference: ReminderPreference): DataResult<Unit>

    suspend fun cancel(): DataResult<Unit>

    /**
     * Re-applies [preference] as if freshly scheduled — used after events that can invalidate an
     * already-scheduled reminder without the user ever touching the preference itself (app reopen,
     * a time-zone change, restoring after an Android device reboot). The default just re-schedules;
     * an implementation may override this to skip redundant platform calls when it can cheaply tell
     * the existing schedule is still correct.
     */
    suspend fun refresh(preference: ReminderPreference): DataResult<Unit> = schedule(preference)
}
