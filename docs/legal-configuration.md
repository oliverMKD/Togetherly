# Legal and support configuration (Step 13.6)

Togetherly ships with **placeholder** legal URLs, not real published documents. Nothing in this
app writes legal policy text pretending it has been reviewed — the in-app Privacy screen (Step
13.5) is a concise summary only; the authoritative documents live externally and must be linked in
before release.

## What must be configured before release

`LegalConfiguration.placeholder()` (`app/application/LegalConfiguration.kt`, registered as a Koin
singleton in `CoreModule.kt`) is the single source for every externally-linked legal/support URL:

| Field | Placeholder value | Replace with |
| --- | --- | --- |
| `privacyPolicyUrl` | `https://example.com/togetherly/privacy` | The published Privacy Policy |
| `termsOfUseUrl` | `https://example.com/togetherly/terms` | The published Terms of Use |
| `subscriptionTermsUrl` | `https://example.com/togetherly/subscription-terms` | The published subscription/auto-renewal terms (App Store/Play Store require this to be linked from any paid-subscription flow) |
| `supportContactUrl` | `null` (row hidden) | A real `mailto:` or `https://` support contact, once one exists |

Before shipping to production, replace `LegalConfiguration.placeholder()` with real values (either
inline, or by threading them through the same per-platform construction site used for
`AppConfiguration` — `androidApp/.../TogetherlyApplication.kt` and
`iosApp/.../TogetherlyIosInitializer.kt` — if the URLs ever need to differ per platform or build
flavor).

## Why placeholders, not blanks

The Legal screen (`feature/family/presentation/LegalScreen.kt`) and the About screen's optional
support link both render unconditionally off `LegalConfiguration`, so every field except
`supportContactUrl` is always non-blank — an empty string would either render a dead row or (worse)
reach `ExternalUrlLauncher` and silently no-op via `isValidExternalUrl`'s scheme check
(`core/net/ExternalUrlLauncher.kt`), which never crashes on an invalid or empty URL but also never
tells anyone why the link did nothing. A recognizably-fake `example.com` placeholder makes a
not-yet-configured link obvious in testing instead.

## Open-source licenses

See `docs/open-source-licenses.md` for how the bundled open-source notices list is maintained and
regenerated — a separate concern from the legal URLs above, since it's generated from actual
resolved dependencies rather than configured per release.
