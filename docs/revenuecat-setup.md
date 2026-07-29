# RevenueCat / Family Plus Setup (Step 11)

This documents the RevenueCat integration for Family Plus: where keys go, what the RevenueCat
dashboard must already have configured, how to test purchases without spending real money, and
how to troubleshoot the integration. For the feature's own architecture (data source, repository,
paywall, contextual triggers, parental gate), see the code itself — this file is operational setup,
not a design doc. For RevenueCat's own PostHog integration (dashboard wiring, the anonymous
identity link, and the low-risk customer attributes this app sets), see
[revenuecat-posthog-integration.md](revenuecat-posthog-integration.md) — a separate concern from
this file's own purchase/entitlement setup.

## 1. API keys — where they go, and why the secret key is never involved

RevenueCat issues a **public** SDK key per platform (Project settings → API keys → "Public
app-specific key") and a separate **secret** key (used only for RevenueCat's own server-side REST
API). Togetherly's client app has no use for the secret key at all — it is never referenced,
stored, or built into this repository in any form, on either platform. Only the public key is
used, and even that is never hardcoded in shared Kotlin source or committed to version control.

**Android**: place the key in `local.properties` (already gitignored):
```
revenueCat.androidApiKey=your_android_public_sdk_key_here
```
See `local.properties.example` for the tracked template. `androidApp/build.gradle.kts` reads this
into `BuildConfig.REVENUECAT_API_KEY`, and `TogetherlyApplication` supplies it to the shared module
via Koin (`RevenueCatApiKeyProvider`).

**iOS**: place the key in `iosApp/Configuration/RevenueCat.local.xcconfig` (already gitignored):
```
REVENUECAT_API_KEY=your_ios_public_sdk_key_here
```
See `RevenueCat.local.xcconfig.example` for the tracked template. `Config.xcconfig` includes this
file if present; `Info.plist` exposes the value via `$(REVENUECAT_API_KEY)`, and
`TogetherlyIosInitializer` reads it from `NSBundle.mainBundle` at startup.

If the key is missing or blank:
- **Debug builds fail loudly** at startup (`RevenueCatConfigurator` throws via `check(...)`), so a
  missing key during development is never silently invisible.
- **Release builds fall back to free mode** (`PurchaseStartupState.NotConfigured`) rather than
  crashing a family's install over a packaging mistake — every free feature (quests, memories,
  Journey, Quest Mode) keeps working; only Family Plus purchase/restore/paywall features are
  unavailable until the key is fixed.

The key itself (complete or partial) is never written to any log.

## 2. Required RevenueCat dashboard configuration

This code assumes the dashboard is already configured with:
- **Entitlement id**: `family_plus` — the sole source of truth for premium access. The app never
  checks whether a specific product was purchased; it only ever checks whether `family_plus` is in
  `CustomerInfo.entitlements.active`.
- **Offering id**: `default` — the app prefers `Offerings.current`, and falls back to
  `Offerings.all["default"]` if RevenueCat ever reports a different offering as current without
  this app's own config changing first.
- **Products**: `togetherly_monthly`, `togetherly_annual`, `togetherly_lifetime`, each attached to
  the `family_plus` entitlement.
- **Packages** (inside the `default` offering): one package per product above, using RevenueCat's
  standard `$rc_monthly`/`$rc_annual`/`$rc_lifetime` package types so the app's own
  `PurchasePackageType`/`BillingPeriod` mapping (`MONTHLY`/`ANNUAL`/`LIFETIME`) resolves correctly.
  A package of an unrecognized type is safely skipped (never crashes the offering load) but won't
  be purchasable from this app's paywall until it's given one of these three types.

**Deferred to Google Play Console / App Store Connect** (outside this repository and outside
RevenueCat's own dashboard): creating the actual in-app product/subscription listings with their
IDs, prices, and localizations on each store; enrolling license testers (Play) or sandbox testers
(App Store Connect); submitting the app for review with in-app purchases declared. RevenueCat only
mirrors what each store already has configured — it cannot create store-side products for you.

## 3. Anonymous identity (no account system yet)

Togetherly has no login/account system, so every family is identified only by RevenueCat's own
auto-generated anonymous app user ID (`Purchases.configure` is called with no `appUserId`). The
native SDK generates this ID once and persists it locally, reusing it across app launches on its
own — `RevenueCatConfigurator` never generates a custom ID and never calls `logIn()`/`logOut()`.

**If Togetherly adds accounts later**, migrating is a single additional call —
`Purchases.sharedInstance.logIn(accountId)` — made once, at the moment an account is created or
signed into. RevenueCat then merges the anonymous purchase history into the new identity
automatically. Nothing in the current integration needs to change to support this; it is a future
call site, not a rework of anything documented here.

## 4. Testing without real money: RevenueCat Test Store

RevenueCat's Test Store (dashboard → a project's "Test Store" toggle, or a dedicated Test Store
API key depending on your RevenueCat account setup) lets every purchase/restore flow in this app be
exercised end-to-end with **no real App Store/Play Store transaction and no sandbox tester
account**. Point the debug build's public key at a Test Store–enabled project (or a Test Store key,
if your RevenueCat plan issues one separately) to use it.

**What to manually verify with the Test Store**, once configured:
1. Launch the app in debug — confirm `RevenueCatConfigurator` reaches `PurchaseStartupState.Ready`
   (check the debug log tag `RevenueCat`).
2. Open the Family Plus paywall (Today's reroll limit, a locked premium quest — or, skipping the
   parental gate, Family tab → "View plans") and confirm all three packages load with
   store-localized titles/prices.
3. **Purchase success**: select a package, tap the primary button, confirm the Test Store's
   simulated purchase sheet, and confirm the paywall switches to its "already premium" state with
   the warm success copy, and that `family_plus` shows active in the RevenueCat dashboard's
   customer view for this anonymous user.
4. **Purchase cancellation**: start a purchase and cancel from the Test Store sheet — confirm the
   paywall shows the gentle "no worries, try again anytime" message, not an alarming error, and
   that access remains free.
5. **Restore**: on a fresh install (or after clearing app data) with the same Test Store
   account/user, open Family Plus management → "Restore purchases" — confirm it reports Family
   Plus restored if the Test Store has a prior purchase for that identity, or the "no purchases
   found" message if not.
6. **Offline/degraded**: with a previously-confirmed premium install, turn off network access and
   relaunch — confirm cached premium access still works for quests/rerolls (subject to the 72-hour
   offline grace period — see `QuestAccessPolicy`), and that the paywall's own package load shows a
   retryable "unable to load plans" error rather than crashing or hanging.

## 5. Customer Center configuration

The "Manage subscription" action on the Family Plus management screen opens RevenueCat's own
prebuilt Customer Center UI (`purchases-kmp-ui`'s `CustomerCenter` composable) — Togetherly builds
no custom cancellation/refund flow of its own. Customer Center's own content (which actions it
offers, its own copy) is configured **in the RevenueCat dashboard**, not in this codebase — see
RevenueCat's Customer Center dashboard section to enable/customize it per project. This app only
decides *when* to show it (a parent-initiated tap, never automatic) and *whether* to show the
"Manage subscription" entry point at all (gated on `EntitlementRepository.isReady()` — if RevenueCat
isn't configured/ready, the button instead shows "Subscription management isn't available right now"
rather than opening a broken screen). Not every store action Customer Center could offer is
guaranteed available on every platform/store — that's RevenueCat's own platform-support boundary,
not something this app's code works around.

## 6. Troubleshooting

- **Empty offering / paywall shows no packages**: confirm the `default` offering has at least one
  package attached in the dashboard, and that each package's product is approved/available on the
  store you're testing against (a pending Play Console review, for example, blocks a real product
  from loading even though it loads fine against a Test Store).
- **Product unavailable during purchase**: the tapped package's product id doesn't match any
  product in the currently-loaded offering — usually a dashboard/store product id mismatch, or the
  offering was reloaded with a different current offering mid-session. Reload the paywall (Retry)
  before assuming it's a code bug.
- **Invalid API key**: `RevenueCatConfigurator` logs a configuration failure and the app falls back
  to `PurchaseStartupState.Failed`/free mode; double-check the key was copied from "Public
  app-specific key" (not the secret key, and not a different project's key) into
  `local.properties`/`RevenueCat.local.xcconfig`.
- **Missing `family_plus` entitlement**: if a product purchase succeeds but isn't attached to the
  `family_plus` entitlement in the dashboard, `CustomerInfo.entitlements.active` will never include
  it — the app will correctly (if confusingly, from a tester's perspective) treat the family as
  still free. Attach every purchasable product to `family_plus` in the dashboard.
- **Purchase succeeds but premium stays locked**: this app deliberately never trusts a completed
  store transaction alone — it only reports success once `CustomerInfo` (returned from the same
  purchase call) actually shows `family_plus` active (see `DefaultRevenueCatDataSource.purchase`).
  If this happens, it means the transaction completed on the store but RevenueCat's own backend
  didn't activate the entitlement — check the entitlement/product/offering attachment above, and
  check the RevenueCat dashboard's customer view for that transaction.
- **Restore finds no purchases**: expected and correctly reported (not an error) whenever the
  current anonymous identity truly has no prior purchase RevenueCat can find — this is common when
  testing restore on a fresh install with a *different* anonymous ID than the one that purchased.
  Reinstalling doesn't preserve the anonymous ID on either platform by itself.
- **Customer Center unavailable**: shown instead of a broken screen whenever
  `EntitlementRepository.isReady()` is false (RevenueCat never configured, or configuration
  failed) — fix the underlying configuration issue above; this message is not itself a bug.
