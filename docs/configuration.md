# Configuration & Secret Classification (Step 14.5.2)

A single reference for every configuration value this project uses, written for public
publication: where it lives, who can see it, and what happens if it's missing. See
`docs/revenuecat-setup.md`, `docs/analytics-setup.md`, and `docs/sentry-setup.md` for the full
per-provider setup instructions — this file only classifies and cross-references them.

## 1. The four categories

| Category | Meaning | Where it lives |
|---|---|---|
| **Client configuration** | Ships inside the compiled app. Public/client-safe by the vendor's own design — a mobile SDK key, not a server secret — but still never hardcoded or committed, so it can be rotated without a code change and never accidentally points a fork at your own project. | `local.properties` (gitignored) / `iosApp/Configuration/*.local.xcconfig` (gitignored) |
| **GitHub secret** | Only needed once this project gains a GitHub Actions pipeline (none exists yet, per Step 14.5.1's audit). Stored in the repository's **Settings → Secrets and variables → Actions**, never in a tracked file. | GitHub repository/organization secret store |
| **CI-only secret** | Used only by build tooling (mapping/dSYM upload), never read by the app itself at runtime. A subset of "GitHub secret" once CI exists on GitHub specifically; listed separately here because the *scope* (build-time only, app never touches it) is the important property, independent of which CI provider eventually hosts it. | CI environment variables only |
| **Never stored in the repository** | Under no circumstances — not gitignored-local, not CI, not anywhere in this repo's history. | Vendor dashboards / a password manager only |

## 2. Client configuration (all four, mirrored Android/iOS)

| Value | Android key (`local.properties`) | iOS key (`*.local.xcconfig`) | Missing behavior |
|---|---|---|---|
| RevenueCat public SDK key | `revenueCat.androidApiKey` | `REVENUECAT_API_KEY` (`RevenueCat.local.xcconfig`) | Free mode, RevenueCat disabled (debug build fails loudly instead — see `docs/revenuecat-setup.md`) |
| PostHog project token | `posthog.projectKey` | `POSTHOG_PROJECT_KEY` (`PostHog.local.xcconfig`) | Analytics fully disabled (no-op), app fully usable |
| PostHog ingestion host | `posthog.host` | `POSTHOG_HOST` (`PostHog.local.xcconfig`) | Defaults to PostHog Cloud EU |
| Sentry DSN | `sentry.dsn` | `SENTRY_DSN` (`Sentry.local.xcconfig`) | Diagnostics fully disabled (no-op), app fully usable |

Every one of these is genuinely public/client-safe by the vendor's own design (see each value's
own setup doc for the reasoning) — the project still keeps them out of tracked source specifically
so a fork or clone never accidentally reports data into *this* project's own RevenueCat/PostHog/
Sentry projects. Tracked, placeholder-only templates exist for every one of the four files above:
`local.properties.example`, `iosApp/Configuration/RevenueCat.local.xcconfig.example`,
`iosApp/Configuration/PostHog.local.xcconfig.example`, `iosApp/Configuration/Sentry.local.xcconfig.example`.

## 3. CI-only secrets (not yet in use — no CI pipeline exists yet)

| Value | Purpose | Scope |
|---|---|---|
| `SENTRY_AUTH_TOKEN` | Authenticates ProGuard/R8 mapping and iOS dSYM upload to Sentry | CI/build tooling only — see `docs/sentry-setup.md` § Release tooling |
| `SENTRY_ORG` | Sentry organization slug for the same upload step | CI/build tooling only |
| `SENTRY_PROJECT` | Sentry project slug for the same upload step | CI/build tooling only |

None of these three exist anywhere in this repository today (`docs/sentry-setup.md` documents them
as the plan for when Android release minification is enabled). The app itself never reads any of
them — only a future CI job's mapping/dSYM upload step would.

## 4. GitHub secrets (once a GitHub Actions pipeline exists)

No `.github/workflows` exist yet, so nothing is stored as a GitHub secret today. When a pipeline is
added, these become genuine `Settings → Secrets and variables → Actions` entries, never tracked
files:

| Value | Purpose |
|---|---|
| Android release keystore (base64-encoded) + its password, key alias, and key password | Signing a release build in CI — no signing config exists in `androidApp/build.gradle.kts` yet |
| Apple Distribution certificate + provisioning profile, or an App Store Connect API key (`.p8` + issuer ID + key ID) | Signing/notarizing/uploading an iOS build in CI |
| `SENTRY_AUTH_TOKEN` / `SENTRY_ORG` / `SENTRY_PROJECT` | Same three CI-only secrets above, once CI specifically runs on GitHub Actions |

## 5. Never stored in the repository, under any circumstances

- RevenueCat **secret** API key (`sk_...`) — this app's mobile SDK only ever needs the public key;
  a secret key belongs to a server-side integration this project doesn't have.
- PostHog **personal** API key (distinct from the project token above).
- Sentry auth token committed to a tracked file (see § 3 — CI environment variable only).
- Any real Android keystore file, keystore password, key alias password, or signing configuration.
- Any real Apple `.p8`/`.p12`/`.mobileprovision` signing material.
- `google-services.json` / `GoogleService-Info.plist` — not applicable today (no Firebase
  integration exists), but never to be added if one ever is.
- OAuth client secrets, private certificates, GitHub personal access tokens.
- Any family-identifying data, memory content, photo, or voice recording — see `docs/privacy.md`
  and `docs/private-media.md` for why none of this can exist outside a device's own private
  filesystem in the first place.

## 6. Verifying a value's classification

If a new integration is added later, classify it against this same table before writing any setup
doc for it — a value is "client configuration" only if the vendor itself explicitly documents it as
safe to ship inside a compiled app (check their own docs, don't assume from the name alone).
