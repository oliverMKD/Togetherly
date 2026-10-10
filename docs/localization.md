# Localization

Togetherly uses Compose Multiplatform resources for production UI copy and currently ships feature
translations in five languages:

| Language | Code | Resource directory |
|---|---|---|
| English (source) | `en` | `shared/src/commonMain/composeResources/values/` |
| Macedonian | `mk` | `shared/src/commonMain/composeResources/values-mk/` |
| German | `de` | `shared/src/commonMain/composeResources/values-de/` |
| French | `fr` | `shared/src/commonMain/composeResources/values-fr/` |
| Spanish | `es` | `shared/src/commonMain/composeResources/values-es/` |

The files are split by feature (`strings_today.xml`, `strings_family.xml`, and so on) rather than
kept in one large resource file. `togetherly_translations_review.csv` is a review artifact that
places the source and translated values side by side; the XML resources remain the runtime source
of truth.

The localized directories currently mirror the feature resource files. Design-system microcopy in
`values/strings.xml` still falls back to English, and `strings_debug.xml` is intentionally
English-only because it is reachable only from debug telemetry tooling. Translating the shared
design-system file is the remaining production-copy gap.

## In-app language selection

The Language screen is opened from the Family tab and offers the five explicit languages plus
**System default**. `SupportedLanguage` owns the supported ISO 639-1 codes and endonyms.
`AppLocaleController` applies a selection and exposes it as a `StateFlow`; `AppLocaleStore`
persists the explicit choice independently of the family profile so it is available before
onboarding and before the first Compose frame.

- Android stores `selected_language` in the private `app_locale` `SharedPreferences` file.
  `Locale.setDefault` applies a selection immediately, and `App` remounts the content below its
  locale key while preserving both navigation controllers.
- iOS stores `selected_language` in `NSUserDefaults` and writes the standard `AppleLanguages`
  preference. Compose resources cannot be reliably hot-swapped through a public API on iOS, so
  the screen shows a restart-required banner and the new locale takes effect on the next launch.
- Selecting **System default** removes the explicit preference. Android restores the process's
  captured startup locale; iOS removes `AppleLanguages` for the next launch.

The language preference is app-level state, not family data. It is intentionally not stored in
Room and is not deleted by family-profile or quest-history cleanup.

## Resource rules

- Put user-facing copy in Compose resources; do not hardcode it in Kotlin.
- Use `stringResource` at composition time and `UiText.Resource` in ViewModel state.
- Use plural resources for counts and platform locale-aware formatters for dates, times, prices,
  and renewal dates. RevenueCat's formatted price is displayed unchanged.
- Do not translate analytics event names, database keys, route identifiers, enum storage keys, or
  catalogue IDs.
- Language names in the picker are endonyms (`Deutsch`, `Français`, `Македонски`) and therefore
  remain plain values in `SupportedLanguage`.
- Keep every translated feature file's resource keys identical to its English source file.

## Adding a language

1. Add the language to `SupportedLanguage` with its ISO code, flag, and endonym.
2. Create `values-<code>/` under `shared/src/commonMain/composeResources`.
3. Copy every English production resource file, including the shared design-system strings, and
   translate values without changing resource names, format placeholders, XML escaping, or plural
   quantities. `strings_debug.xml` may remain source-only.
4. Add the language column to `togetherly_translations_review.csv` if that review artifact is
   still being maintained.
5. Run the shared tests and compile both Android and iOS targets.
6. Manually verify onboarding, bottom navigation, filters, settings, paywall, dialogs, and memory
   controls at normal and large font scales.

## Verification

Automated coverage includes `AppLocaleControllerTest`, locale-aware date/time formatter tests,
resource compilation on Android and iOS, and production Koin graph resolution for
`LanguageViewModel`. Useful commands are:

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
```

Manual checks remain important for translated text expansion, 12/24-hour formatting, currency,
TalkBack/VoiceOver reading order, and RTL safety if an RTL language is added later.
