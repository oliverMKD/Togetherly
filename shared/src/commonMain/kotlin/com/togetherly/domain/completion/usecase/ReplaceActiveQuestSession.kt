package com.togetherly.domain.completion.usecase

import com.togetherly.core.datetime.AppClock
import com.togetherly.core.error.AppError
import com.togetherly.core.error.ContentError
import com.togetherly.core.error.ValidationError
import com.togetherly.core.id.IdGenerator
import com.togetherly.core.result.DataResult
import com.togetherly.domain.completion.ActiveQuestSession
import com.togetherly.domain.completion.CompletionId
import com.togetherly.domain.completion.repository.QuestSessionTransaction
import com.togetherly.domain.family.repository.FamilyRepository
import com.togetherly.domain.quest.QuestId
import com.togetherly.domain.quest.repository.QuestRepository
import com.togetherly.domain.validation.DomainValidationException

/**
 * The explicit, user-confirmed counterpart to [StartQuest] noticing
 * [com.togetherly.core.error.ValidationError.ACTIVE_SESSION_CONFLICT] on its own — this only runs
 * once a family has been shown the conflict (another quest is still in progress) and deliberately
 * chosen to replace it, never as a silent fallback inside [StartQuest] itself (see that class's own
 * KDoc for why). Whatever the previously active session was tracking is abandoned without being
 * saved as a completion — replacing is not the same as completing it.
 */
class ReplaceActiveQuestSession(
    private val familyRepository: FamilyRepository,
    private val questRepository: QuestRepository,
    private val questSessionTransaction: QuestSessionTransaction,
    private val clock: AppClock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(questId: QuestId): DataResult<ActiveQuestSession> {
        val profileResult = familyRepository.getProfile()
        if (profileResult is DataResult.Error) return profileResult
        val profile = (profileResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Validation(ValidationError.MISSING_FAMILY_PROFILE))

        val questResult = questRepository.getQuest(questId)
        if (questResult is DataResult.Error) return questResult
        val quest = (questResult as DataResult.Success).value
            ?: return DataResult.Error(AppError.Content(ContentError.QUEST_NOT_FOUND))

        val session = try {
            ActiveQuestSession(
                completionId = CompletionId(idGenerator.generate()),
                familyId = profile.id,
                questId = quest.id,
                questVersion = quest.version,
                startedAt = clock.now(),
            )
        } catch (e: DomainValidationException) {
            return DataResult.Error(AppError.Validation(ValidationError.INVALID_INPUT))
        }

        val replaceResult = questSessionTransaction.replaceActiveSession(session)
        if (replaceResult is DataResult.Error) return replaceResult

        return DataResult.Success(session)
    }
}
