# RevenueCat ↔ PostHog integration (Step 14.4)

This documents the dashboard-side connection between RevenueCat and PostHog, and the app-side
identity link and customer attributes that back it. See [telemetry.md](telemetry.md) for the
consent/provider architecture, [analytics-setup.md](analytics-setup.md) for the PostHog SDK itself,
and [analytics-event-taxonomy.md](analytics-event-taxonomy.md) for every event this app sends —
this file is dashboard configuration and the RevenueCat-specific identity/attribute code that
supports it, not a repeat of either.

## Two separate sources of truth

**PostHog app events show intent and UX outcomes.** `paywall_presented`, `purchase_started`,
`purchase_result`, `restore_started`, `restore_result` (see
[analytics-event-taxonomy.md](analytics-event-taxonomy.md#paywall--purchases)) are captured
client-side, the instant a family taps something or a store SDK call returns. They can be cancelled,
can fail before ever reaching a store, and — like any client-side event — can be lost if the app is
killed mid-flow before a flush.

**RevenueCat events show validated subscription lifecycle and revenue.** RevenueCat only ever
records a transaction after validating the receipt with Apple/Google (or its own billing), and every
subsequent lifecycle change (a renewal, a cancellation, a billing issue, an expiration) is generated
server-side as RevenueCat reconciles against the store — never invented client-side.

**Do not use `purchase_result(success)` as the authoritative revenue metric.** It is a real, useful
product-intent signal (conversion rate through the purchase funnel, time-to-purchase, which paywall
context converts best), but it is not a receipt. Revenue reporting, MRR, churn, and subscriber counts
must always be read from RevenueCat itself (its dashboard, or its own PostHog-forwarded `rc_*`
events — see below) — never derived from this app's own `purchase_result` event stream.

## RevenueCat → PostHog event forwarding

RevenueCat's own official PostHog integration forwards a fixed set of subscription lifecycle events
into the *same* PostHog project this app's SDK sends to, under a `rc_`-prefixed, `_event`-suffixed
naming convention:

| RevenueCat lifecycle event | Forwarded PostHog event |
|---|---|
| Initial purchase | `rc_initial_purchase_event` |
| Trial started | `rc_trial_started_event` |
| Trial converted | `rc_trial_converted_event` |
| Trial cancelled | `rc_trial_cancelled_event` |
| Renewal | `rc_renewal_event` |
| Cancellation | `rc_cancellation_event` |
| Uncancellation | `rc_uncancellation_event` |
| Non-subscription purchase | `rc_non_subscription_purchase_event` |
| Subscription paused | `rc_subscription_paused_event` |
| Expiration | `rc_expiration_event` |
| Billing issue | `rc_billing_issue_event` |
| Product change | `rc_product_change_event` |

For events that carry revenue (initial purchases, trial conversions, renewals), RevenueCat attaches
the amount automatically, converted to USD — this is the revenue figure PostHog's own revenue
insights should be built on, never a value computed from this app's own event properties (which
never carry a price at all — see [analytics-event-taxonomy.md](analytics-event-taxonomy.md#never-capture)).

RevenueCat's own broader webhook event vocabulary is larger than what it forwards to PostHog
(`SUBSCRIPTION_EXTENDED`, `REFUND_REVERSED`, `INVOICE_ISSUANCE`, `TRANSFER`,
`TEMPORARY_ENTITLEMENT_GRANT`, `VIRTUAL_CURRENCY_TRANSACTION`, `PURCHASE_REDEEMED`,
`EXPERIMENT_ENROLLMENT`, `PAYWALL_IMPRESSION`/`PAYWALL_CLOSE`/`PAYWALL_CANCEL`/`PAYWALL_EXIT_OFFER`,
price-increase-consent events) — this app uses none of that broader surface (no RevenueCat-hosted
Paywalls UI, no webhook endpoint of its own); only the PostHog integration's fixed forwarding list
above is relevant here.

## How to avoid double-counting

`purchase_result(success)` and `rc_initial_purchase_event`/`rc_renewal_event` describe the *same
underlying purchase* from two different vantage points — an intent-and-outcome signal and a
verified-revenue signal. They are not duplicates to be deduplicated against each other; they answer
different questions and should stay as two separate event series:

- Use `purchase_result` (grouped by `result`) to measure funnel conversion and UX friction.
- Use `rc_initial_purchase_event`/`rc_renewal_event` (RevenueCat's own revenue field) to measure
  actual revenue, MRR, and subscriber counts.
- **Never sum both series together as if they were independent revenue events** — a single real
  purchase produces exactly one `purchase_result(success)` (this app's own duplicate-purchase
  prevention — see `RevenueCatEntitlementRepository.purchase`'s in-flight coalescing — guarantees
  this) and, once validated, exactly one `rc_initial_purchase_event`. Building a combined "total
  purchases" metric from both would double the true count.
- If a PostHog dashboard needs a single funnel spanning both, use `purchase_started` →
  `purchase_result(success)` as the client-side half and cross-reference against
  `rc_initial_purchase_event` counts for the same period as a validation check, not as an
  additional funnel step.

## Sandbox vs production event forwarding

RevenueCat's PostHog integration forwards sandbox purchases by default (there is no separate
opt-out toggle for sandbox specifically) — every `rc_*` event carries RevenueCat's own `environment`
field (`SANDBOX` or `PRODUCTION`), the same field RevenueCat's webhook payloads use. Before trusting
any PostHog revenue insight built on `rc_*` events:

1. **During development/QA**: expect sandbox events to appear in the same PostHog project as
   production events. Filter any revenue-facing insight to `environment = PRODUCTION` explicitly —
   never assume a project is "clean" of sandbox data just because it's the production PostHog
   project.
2. **Before shipping a build pointed at a production PostHog project**: confirm sandbox test
   purchases made during that verification pass are excluded from any revenue dashboard the team
   actually reads, by the same `environment` filter.
3. There is no server-side way to stop RevenueCat from forwarding sandbox events at all — filtering
   in PostHog (saved insight filters, or a dashboard-level property filter) is the only mechanism.

## RevenueCat dashboard configuration

1. **PostHog project**: use the same PostHog Cloud EU project [analytics-setup.md](analytics-setup.md)
   already configures this app's SDK against — RevenueCat's forwarded events and this app's own
   events must land in one project for funnels spanning both to work at all.
2. **RevenueCat → Project settings → Integrations → PostHog**:
   - Paste the same PostHog **Project API key** [analytics-setup.md](analytics-setup.md#1-creating-a-posthog-cloud-eu-project)
     already documents (the public, client-safe `phc_`-prefixed token — never a PostHog personal API
     key here either).
   - Set the **region** to **EU**, matching this app's own `PostHogConfig.HOST_EU` default — a
     region mismatch would forward RevenueCat's events into a different PostHog cloud than this
     app's own SDK writes to, silently breaking every funnel that spans both.
   - Leave event names at RevenueCat's own defaults (the `rc_*_event` names in the table above) —
     no reason to rename them; funnels/insights referencing the default names stay portable across
     projects/environments.
   - Choose **gross or net revenue** reporting per the team's own finance/reporting preference; this
     app's own client-side events never carry a price either way, so this choice only affects how
     RevenueCat's own forwarded revenue figure is computed, not anything this codebase does.
3. **The `$posthogUserId` attribute**: RevenueCat's own integration reads this reserved subscriber
   attribute to associate its forwarded events with a PostHog identity. This app sets/clears it
   automatically — see [Identity linking](#identity-linking) below; no manual dashboard step is
   needed for this part beyond enabling the integration itself.
4. **Verification**: after enabling, make one sandbox purchase from a debug build (see
   [analytics-setup.md](analytics-setup.md#7-testing-debug-events) for how to grant analytics
   consent during development) and confirm an `rc_initial_purchase_event` appears in PostHog's live
   events view, associated with the same distinct ID this app's own `purchase_started`/
   `purchase_result` events for that same purchase carry.

## Identity linking

Togetherly has no accounts — every install is an anonymous PostHog distinct ID and a separate,
also-anonymous RevenueCat App User ID, generated independently by each SDK. `$posthogUserId` is the
one-directional bridge RevenueCat's own PostHog integration reads to associate its forwarded revenue
events with the *right* PostHog identity instead of falling back to RevenueCat's own (unrelated)
anonymous App User ID.

`data/purchase/RevenueCatAnalyticsLinker.kt` (internal to `shared`) is the one place this ever
happens, started once from `KoinConfiguration.initKoin` alongside `TelemetryCoordinator.start()`:

- **After analytics consent is granted**: reads PostHog's own anonymous distinct ID via
  `PostHogSdkAdapter.anonymousId()` (`PostHog.getAnonymousId()` — never a call to `identify()`,
  since this app never identifies anyone) and writes it onto RevenueCat's `$posthogUserId`
  subscriber attribute via `Purchases.sharedInstance.setAttributes(mapOf("$posthogUserId" to id))`
  (`purchases-kmp` 3.3.1's own documented generic attributes API — confirmed via RevenueCat's
  current KMP SDK reference; there is no PostHog-specific dedicated setter the way there is for
  Mixpanel/OneSignal/Adjust/AppsFlyer, only the reserved `$posthogUserId` key through the generic
  call).
- **Never calls `Purchases.sharedInstance.logIn()`** and never replaces RevenueCat's own anonymous
  App User ID — the subscriber attribute is the entire mechanism; RevenueCat's own identity is
  untouched.
- **Never collects an advertising identifier** anywhere in this flow, or anywhere else in this
  codebase.
- **Never associates before consent** — `RevenueCatAnalyticsLinker` only ever calls
  `setPostHogDistinctId` on an actual `ConsentDecision.Granted` emission from
  `TelemetryConsentRepository.observeConsent()`.
- **On revocation** (a transition out of `Granted`): clears the attribute
  (`setPostHogDistinctId(null)`, RevenueCat's own documented "clear by passing null" mechanism —
  verified via RevenueCat's customer-attributes documentation). This **never touches RevenueCat
  entitlement access** — `RevenueCatAnalyticsLinker` has no dependency on anything but
  `RevenueCatDataSource.setPostHogDistinctId`/`setCustomerAttributes`, never on `getCustomerAccess`/
  `purchase`/`restorePurchases`.
- Every call is wrapped in `runCatching` at two layers (inside `DefaultRevenueCatDataSource` itself,
  and again in the linker) — a RevenueCat SDK failure here can never break a purchase, a restore, or
  app startup.

### Limitations, disclosed rather than assumed away

Clearing `$posthogUserId` on revocation stops *future* RevenueCat-to-PostHog event forwarding from
being associated with that distinct ID. It does **not** retroactively unlink revenue events
RevenueCat already forwarded to PostHog's server side while consent was granted — neither this app
nor RevenueCat's client SDK can reach into PostHog's own server-side event history to undo that.
**This app never promises retroactive deletion from RevenueCat or PostHog in any UI copy**, and no
future consent-management screen should imply otherwise without first confirming the actual
deletion capability with both vendors.

## RevenueCat customer attributes

Three low-risk attributes are written for paywall targeting, each reviewed against the same six
questions before being added:

| Attribute | Required? | Predefined? | Non-sensitive? | Low-cardinality? | Covered by consent? | Needed for targeting? |
|---|---|---|---|---|---|---|
| `onboarding_completed` | Yes — targets families mid-funnel who haven't finished onboarding | Yes — always the literal string `"true"` | Yes — no profile content | Yes — one value | Yes — gated on analytics consent | Yes — lets a paywall campaign exclude/include by onboarding status |
| `first_quest_completed` | Yes — targets families who haven't yet reached the activation milestone | Yes — always `"true"`, written idempotently on every completion | Yes | Yes — one value | Yes | Yes — a distinct paywall message for "activated but not yet premium" vs "not yet activated" |
| `preferred_duration_bucket` | Yes — lets a paywall highlight the package type matching a family's own stated session-length preference | Yes — one of the four `DurationBand` values, lowercased | Yes — a broad bucket, not exact minutes/seconds and not tied to a specific child | Yes — four possible values | Yes | Yes — session-length-aware paywall copy/package ordering |

Implementation: `domain/purchase/repository/CustomerAttributesRepository.kt` (provider-neutral,
exactly these three methods — deliberately not a generic "set any attribute" passthrough, so a new
attribute always goes through this same review table rather than an ad hoc call from feature code)
and `data/purchase/RevenueCatCustomerAttributesRepository.kt` (the implementation, gated on analytics
consent exactly like the identity link above — re-checked fresh on every call, not cached).

- `markOnboardingCompleted()` — called once, from `OnboardingViewModel` at the same
  `CreateFamilyProfile` success moment `onboarding_completed` (the analytics event) fires.
- `markFirstQuestCompleted()` — called from `QuestModeViewModel` on every successful quest
  completion, not only the first. RevenueCat keeps only the latest value for a given attribute key,
  so writing `"true"` again on the second, third, … completion is a harmless no-op in effect —
  detecting "is this genuinely the first completion" would need an extra query for no behavioral
  difference.
- `setPreferredDurationBucket(bucket)` — called once, from `OnboardingViewModel`, using the shortest
  `DurationBand` the family selected during onboarding (`selectedDurations.minOrNull()`). **Not
  re-synced from later preference edits** (`QuestPreferencesViewModel`) — a deliberate, minimal-scope
  decision; re-syncing on every later edit is possible future work if paywall targeting on a stale
  value ever proves to matter in practice.

**Never sent to RevenueCat**: age range, participant count, family preferences beyond the one
duration bucket above, memory behavior (note/photo/voice presence), search history, or anything else
this app's own [Never capture](analytics-event-taxonomy.md#never-capture) list already forbids for
PostHog — the same restraint applies here, if anything more strictly, since RevenueCat attributes are
also visible to whichever team members configure paywall targeting in RevenueCat's own dashboard.
