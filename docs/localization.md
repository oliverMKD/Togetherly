# Togetherly localization readiness

English is still the source language. I did not add translated locale files.

## What is now localized

- Date and time presentation now goes through platform locale-aware formatters instead of manual English month names or fixed `AM`/`PM` strings.
- Reminder and onboarding review copy no longer concatenates user-facing strings in Kotlin.
- Quest-count display now uses plural resources.
- Debug telemetry status labels now come from resources instead of hardcoded English literals.
- Legal and purchase URLs remain data/config driven; they were not localized.
- Analytics identifiers, database identifiers, and stored enum values were left untouched.

## Automated coverage

- Android host tests cover locale-sensitive date/time formatting:
  - 12-hour locale formatting
  - 24-hour locale formatting
  - locale-dependent date formatting
- Journey mapping expectations now match the localized date format.

## Supported behavior

- Locale-aware date display for completion, journey, and renewal dates.
- Locale-aware time display for reminder pickers and journey time labels.
- Pluralized quest counts in Explore and Pack Details.
- Resource-backed reminder, onboarding review, and debug status copy.
- RevenueCat prices continue to display using the SDK’s localized formatting without extra manipulation.

## Remaining manual verification

- Long family names on onboarding and profile editing screens.
- Long quest titles and pack titles in Explore, Pack Details, Quest Details, and Journey.
- Large text / dynamic font scaling on all production screens.
- 12-hour and 24-hour system locale checks on real Android and iOS devices.
- Decimal and currency locale formatting on the paywall and purchase flows.
- RTL layout safety on Android and iOS.
- TalkBack and VoiceOver checks for localized labels and announcements.

## Notes

- I attempted a compose-host verification pass for long-text and RTL cases, but the host compose test environment in this repository was not stable enough to keep that automated check. I removed it rather than leave a flaky test behind.
- No pseudo-localization file was added yet because the project does not already have a localization pipeline beyond Compose resources, and there is no established second locale to mirror.
