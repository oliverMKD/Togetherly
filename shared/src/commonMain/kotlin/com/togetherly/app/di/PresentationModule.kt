package com.togetherly.app.di

import com.togetherly.feature.completion.presentation.CompletionCelebrationViewModel
import com.togetherly.feature.debug.presentation.DebugTelemetryViewModel
import com.togetherly.feature.explore.presentation.ExploreFilterStore
import com.togetherly.feature.explore.presentation.ExploreFiltersViewModel
import com.togetherly.feature.explore.presentation.ExploreViewModel
import com.togetherly.feature.family.presentation.AboutViewModel
import com.togetherly.feature.family.presentation.DataManagementViewModel
import com.togetherly.feature.family.presentation.FamilyProfileEditorViewModel
import com.togetherly.feature.family.presentation.FamilySaveMessageStore
import com.togetherly.feature.family.presentation.FamilyViewModel
import com.togetherly.feature.family.presentation.LegalViewModel
import com.togetherly.feature.family.presentation.MemorySettingsViewModel
import com.togetherly.feature.family.presentation.OpenSourceLicensesViewModel
import com.togetherly.feature.family.presentation.PrivacyViewModel
import com.togetherly.feature.family.presentation.QuestPreferencesViewModel
import com.togetherly.feature.reminder.presentation.ReminderViewModel
import com.togetherly.navigation.shell.RequestedTabStore
import com.togetherly.feature.journey.presentation.JourneyViewModel
import com.togetherly.feature.memory.presentation.CompletionMemoryViewModel
import com.togetherly.feature.familyplus.presentation.FamilyPlusManagementViewModel
import com.togetherly.feature.onboarding.presentation.OnboardingViewModel
import com.togetherly.feature.packdetails.presentation.PackDetailsViewModel
import com.togetherly.feature.paywall.presentation.FamilyPlusPaywallViewModel
import com.togetherly.feature.questdetail.presentation.QuestDetailViewModel
import com.togetherly.feature.questmode.presentation.QuestModeViewModel
import com.togetherly.feature.saved.presentation.SavedViewModel
import com.togetherly.feature.today.presentation.TodayViewModel
import com.togetherly.navigation.state.BootstrapViewModel
import org.koin.dsl.module

/**
 * ViewModels only. Navigation/feature code resolves them through `koinViewModel<T>()` at the
 * `NavHost`/screen boundary (see [com.togetherly.navigation.host.BootstrapScreen]) — never by
 * constructing one directly — so this module is what makes that resolution possible, the same way
 * [repositoryModule] backs `get()` calls in [readyUseCaseModule].
 *
 * [OnboardingViewModel] resolves [com.togetherly.domain.family.usecase.CreateFamilyProfile] from
 * [readyUseCaseModule] — already part of [appModules]. [TodayViewModel] (Step 8.3) resolves
 * entirely from [readyUseCaseModule]/[repositoryModule]/[coreModule] the same way.
 *
 * [QuestDetailViewModel]/[QuestModeViewModel] (Step 8.6, rebuilt in Step 9.3) are the first
 * parameterized factories here — each needs a nav-arg-derived ID (`questId`/`completionId`) that
 * isn't itself a Koin-managed dependency, resolved via `parametersOf` at the call site
 * ([com.togetherly.feature.questdetail.presentation.QuestDetailRoute]/[com.togetherly.feature.questmode.presentation.QuestModeRoute])
 * rather than a `SavedStateHandle` read inside the `ViewModel` itself.
 *
 * [FamilyPlusPaywallViewModel] (Step 11.5) injects [com.togetherly.domain.purchase.repository.EntitlementRepository]
 * directly for its observe/read access rather than through a pass-through use case — matching
 * [TodayViewModel]'s own direct repository injection for the same kind of read access.
 */
val presentationModule = module {
    factory { BootstrapViewModel(get(), get(), get(), get(), get()) }
    factory { OnboardingViewModel(get(), get(), get()) }
    factory { TodayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { params -> QuestDetailViewModel(params.get(), params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { params -> QuestModeViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { params -> CompletionCelebrationViewModel(params.get(), get(), get(), get(), get(), get()) }
    factory { params -> CompletionMemoryViewModel(params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { JourneyViewModel(get(), get(), get(), get(), get(), get()) }
    factory { params -> FamilyPlusPaywallViewModel(params.get(), get(), get(), get(), get()) }
    factory { FamilyPlusManagementViewModel(get(), get(), get(), get()) }
    single { FamilySaveMessageStore() }
    factory { FamilyViewModel(get(), get()) }
    factory { FamilyProfileEditorViewModel(get(), get(), get()) }
    factory { QuestPreferencesViewModel(get(), get(), get()) }
    factory { ReminderViewModel(get(), get(), get(), get(), get(), get()) }
    factory { MemorySettingsViewModel(get(), get(), get()) }
    factory { PrivacyViewModel() }
    factory { LegalViewModel(get()) }
    factory { OpenSourceLicensesViewModel() }
    factory { AboutViewModel(get(), get(), get(), get()) }
    factory { DebugTelemetryViewModel(get(), get(), get(), get(), get(), get()) }
    factory { DataManagementViewModel(get(), get(), get()) }
    single { RequestedTabStore() }
    single { ExploreFilterStore() }
    factory { ExploreViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { ExploreFiltersViewModel(get(), get()) }
    factory { SavedViewModel(get(), get(), get(), get()) }
    factory { params -> PackDetailsViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
}
