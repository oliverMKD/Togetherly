# Configuration and secrets

Togetherly keeps mobile SDK configuration out of tracked source so a clone or fork never reports
to this project's RevenueCat, PostHog, or Sentry projects. These values are client-visible in a
compiled mobile app; they are configuration, not server secrets.

## Local files

| Platform | Tracked template | Local file |
|---|---|---|
| Android | `local.properties.example` | `local.properties` |
| iOS RevenueCat | `iosApp/Configuration/RevenueCat.local.xcconfig.example` | `RevenueCat.local.xcconfig` |
| iOS PostHog | `iosApp/Configuration/PostHog.local.xcconfig.example` | `PostHog.local.xcconfig` |
| iOS Sentry | `iosApp/Configuration/Sentry.local.xcconfig.example` | `Sentry.local.xcconfig` |

The local files are gitignored. Copy the templates and fill only the integrations you need.

## Runtime values

| Integration | Android key | iOS key | Missing behavior |
|---|---|---|---|
| RevenueCat | `revenueCat.androidApiKey` | `REVENUECAT_API_KEY` | Debug startup fails clearly; release continues in free mode. |
| PostHog token | `posthog.projectKey` | `POSTHOG_PROJECT_KEY` | Product analytics uses a no-op implementation. |
| PostHog host | `posthog.host` | `POSTHOG_HOST` | Defaults to PostHog Cloud EU. |
| Sentry DSN | `sentry.dsn` | `SENTRY_DSN` | Operational diagnostics uses a no-op implementation. |

See [revenuecat-setup.md](revenuecat-setup.md), [analytics-setup.md](analytics-setup.md), and
[sentry-setup.md](sentry-setup.md) for provider setup and testing.

## Secrets and release credentials

The app never needs a RevenueCat secret key, PostHog personal API key, or Sentry auth token at
runtime. Do not place any of them in the local files above.

Release signing material and upload credentials belong in a password manager or CI secret store,
never in the repository:

- Android keystore, passwords, and aliases
- Apple distribution certificates, provisioning profiles, and App Store Connect API keys
- `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, and `SENTRY_PROJECT` for future mapping/dSYM upload tooling

Current GitHub Actions builds do not consume repository secrets. They compile with blank runtime
configuration and without distribution signing; see [continuous-integration.md](continuous-integration.md)
and the platform release guides.

## Classification rule

Treat a new value as client configuration only when the vendor explicitly documents it as safe to
ship in a compiled mobile app. Otherwise keep it out of application configuration and document
the server/CI boundary before adding the integration.
