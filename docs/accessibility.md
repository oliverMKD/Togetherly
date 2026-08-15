# Accessibility Audit

This note records the current accessibility baseline for Togetherly after the production-screen audit.

## Supported Behavior

- Production top bars now use a shared localized back-label resource instead of hardcoded English text.
- Shared button, chip, card, text-field, loading, and inline-error components already expose the expected Compose semantics:
  - interactive controls have labels and roles
  - selected states are announced through semantics
  - loading state uses `stateDescription`
  - error state uses accessibility error semantics
  - compact chips and icon buttons keep a 48dp minimum touch target
- Decorative content stays out of the accessibility tree where it should:
  - the completion star animation is semantics-free
  - Journey's constellation header is announced as one combined element, not one star at a time
- Private memory content is not leaked through semantics:
  - the memory photo thumbnail is labeled as the photo itself
  - the underlying private reference string is not exposed in the tree
- The screens audited for this pass are covered by existing Compose semantics patterns and the new device tests added here:
  - Pack Details back navigation
  - Completion Memory private-photo semantics

## Remaining Manual Checks

These still need hands-on TalkBack and VoiceOver verification on real devices:

- Onboarding: step-by-step traversal, error announcement timing, large-font review layout.
- Today: filter sheet traversal, reveal-to-revealed transition, reroll confirmation, timer announcement behavior.
- Quest Details: locked-state narration, save-toggle semantics, conflict dialog focus order.
- Quest Mode: timer live-region behavior, hints expand/collapse state, exit/abandon dialog focus trapping.
- Completion: final celebration screen reading order, add-memory flow, error announcements.
- Memory creation: note field, photo import/remove flow, voice recording controls, discard dialog.
- Journey: delete action, voice playback controls, photo content descriptions, empty state.
- Explore: search/filters traversal, locked card narration, lazy-list keyboard navigation, saved action.
- Pack Details: heading order, locked/unlocked state narration, quest-card traversal.
- Paywall: package selection semantics, restore/purchase busy states, premium state.
- Family settings and dialogs: toggle groups, destructive actions, confirmation dialogs, and keyboard traversal where relevant.

## Verification

- Passed: `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :androidApp:compileReleaseKotlin :androidApp:lintDebug`
- Blocked externally: `./gradlew :shared:compileAndroidDeviceTest`
  - The Android device-test source set still has unrelated pre-existing compilation errors, including missing `diagnostics`/`analytics` parameters in existing tests and an unresolved `LOVE` reference in `RoomMemoryCleanerTest`.
  - The accessibility test files added for this audit were fixed during the pass; the remaining failure is outside the accessibility changes.
