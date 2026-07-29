<!--
Thanks for contributing to Togetherly. Fill in what's relevant — most sections are optional for a
small/unrelated change (a typo fix doesn't need a privacy-impact writeup). Delete any section that
doesn't apply.
-->

## Summary

<!-- One or two sentences: what does this PR do? -->

## Why

<!-- What problem does this solve, or what does it enable? Link an issue if there is one. -->

## Changes

<!-- Bullet list of the actual changes, if more than a sentence's worth. -->

## Screenshots / video

<!-- Required for any UI change. Before/after if it's a visual change. -->

## Testing

<!-- How did you verify this? What did you run? -->

- [ ] Android verification (`./gradlew :androidApp:assembleDebug` / manual run)
- [ ] iOS verification (Xcode build / manual run) — see [docs/architecture.md](../docs/architecture.md)
- [ ] `./gradlew allTests` passes locally

## Privacy impact

<!-- Only relevant if this touches family data, memory content, or local storage. See
docs/privacy.md and docs/local-data-deletion.md. Leave blank/delete if not applicable. -->

## Analytics impact

<!-- Only relevant if this adds/changes an AnalyticsEvent or diagnostics capture boundary. See
docs/telemetry.md and docs/analytics-event-taxonomy.md. Leave blank/delete if not applicable. -->

## RevenueCat impact

<!-- Only relevant if this touches purchase/entitlement behavior. See docs/revenuecat-setup.md.
Leave blank/delete if not applicable. -->

## Checklist

- [ ] No real API keys, DSNs, or other configuration values are included (see [docs/configuration.md](../docs/configuration.md))
- [ ] No real or realistic family/memory content (photos, voice recordings, personal names) in test fixtures or screenshots
- [ ] Relevant docs updated, if behavior changed
