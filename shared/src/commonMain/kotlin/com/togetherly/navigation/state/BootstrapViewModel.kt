package com.togetherly.navigation.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.togetherly.core.datetime.AppClock
import com.togetherly.core.notification.ReminderScheduler
import com.togetherly.core.result.DataResult
import com.togetherly.core.telemetry.DiagnosticContext
import com.togetherly.core.telemetry.OperationalDiagnostics
import com.togetherly.core.telemetry.toDiagnosticThrowable
import com.togetherly.core.ui.toUiText
import com.togetherly.data.media.PendingMediaOrphanCleaner
import com.togetherly.domain.family.FamilyProfile
import com.togetherly.domain.family.repository.FamilyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Decides Bootstrap's outcome purely from [FamilyRepository.observeProfile] — never from a stored
 * "has onboarded" boolean, so a family profile deleted outside the app's own flow (a fresh
 * install restoring an old backup, a support-driven data reset) is reflected immediately rather
 * than trusting stale local state. A `null` profile means [BootstrapUiState.RequiresOnboarding]; a
 * present one means [BootstrapUiState.Ready] — see [FamilyRepository]'s own KDoc for why `null` is
 * never a distinct error.
 *
 * [FamilyRepository.observeProfile]'s Flow terminates after emitting a [DataResult.Error] (its
 * `catch` operator emits the error and completes rather than resuming) — it does not recover on
 * its own. [retry] re-subscribes a fresh call via [retrySignal] rather than assuming the existing
 * Flow will resume by itself.
 *
 * Also fires [PendingMediaOrphanCleaner.deleteExpiredPending] once, best-effort, on construction —
 * Bootstrap is visited exactly once per app process (Step 10.7 privacy/filesystem audit), the same
 * reason a stray pending photo/voice file abandoned by a crashed or killed memory-capture session
 * would otherwise never get swept: nothing else in the app currently calls this cleaner. Its
 * result is intentionally ignored — a failed sweep must never block or degrade the onboarding/
 * ready decision above, which is this ViewModel's actual job.
 *
 * Also fires [ReminderScheduler.refresh] once, best-effort, on construction (Step 13.4) — the
 * self-healing path for a reminder schedule an Android reboot cleared before
 * [com.togetherly.core.notification.ReminderBootReceiver] could run (app not yet installed at
 * boot time is impossible, but a schedule silently dropped by the OS for other reasons isn't), or
 * for a time-zone change since the last schedule. A family that never enabled reminders means
 * [FamilyProfile.reminderPreference] is `null` — nothing to refresh, silently skipped.
 *
 * Bootstrap is also Togetherly's own analog to "navigation restoration": it is the one place that
 * decides, on every cold start, which destination the app resumes into from persisted state. A
 * [DataResult.Error] here — this app has no other framework-level navigation-state-restoration
 * hook to instrument (see `docs/sentry-setup.md`) — is captured via [diagnostics] before being
 * turned into [BootstrapUiState.Error].
 */
class BootstrapViewModel(
    private val familyRepository: FamilyRepository,
    private val pendingMediaOrphanCleaner: PendingMediaOrphanCleaner,
    private val reminderScheduler: ReminderScheduler,
    private val clock: AppClock,
    private val diagnostics: OperationalDiagnostics,
) : ViewModel() {

    private val retrySignal = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    init {
        viewModelScope.launch {
            pendingMediaOrphanCleaner.deleteExpiredPending(now = clock.now())
        }
        viewModelScope.launch {
            val profile = (familyRepository.getProfile() as? DataResult.Success)?.value
            profile?.reminderPreference?.let { reminderScheduler.refresh(it) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BootstrapUiState> = retrySignal
        .flatMapLatest { familyRepository.observeProfile() }
        .map { result -> result.toBootstrapUiState(diagnostics) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BootstrapUiState.Loading,
        )

    fun retry() {
        retrySignal.tryEmit(Unit)
    }
}

private fun DataResult<FamilyProfile?>.toBootstrapUiState(diagnostics: OperationalDiagnostics): BootstrapUiState = when (this) {
    is DataResult.Success -> if (value != null) BootstrapUiState.Ready else BootstrapUiState.RequiresOnboarding
    is DataResult.Error -> {
        diagnostics.captureHandledException(
            error.toDiagnosticThrowable(),
            DiagnosticContext(mapOf("feature" to "navigation", "operation" to "bootstrap_restore")),
        )
        BootstrapUiState.Error(error.toUiText())
    }
}
