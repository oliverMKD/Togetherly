# Togetherly purchase and entitlement edge cases

This pass audits the RevenueCat-backed Family Plus flow. Product identifiers remain unchanged:

- Entitlement: `family_plus`
- Offering: `default`
- Products: `togetherly_monthly`, `togetherly_annual`, `togetherly_lifetime`

## What the app already does

- Uses `family_plus` as the entitlement source of truth.
- Uses the current RevenueCat offering first, with `default` as the documented fallback.
- Does not hardcode prices in purchase logic.
- Treats cancellation as a normal outcome.
- Keeps free functionality available when RevenueCat is missing, unavailable, or misconfigured.
- Does not send receipts, transaction identifiers, or purchase tokens to analytics.
- Does not cancel subscriptions when local data is deleted; only the local cache is cleared.
- Keeps purchase restoration possible after local reset.

## Automated coverage

| Case | Status | Existing coverage |
| --- | --- | --- |
| RevenueCat unavailable at startup | Automated pass | `KoinConfigurationTest`, `DebugTelemetryViewModelTest` |
| Missing API key | Automated pass | `RevenueCatConfigurator` behavior via `DebugTelemetryViewModelTest` |
| Empty current offering | Automated pass | `FamilyPlusPaywallViewModelTest` package-load failure path |
| Missing monthly package | Automated pass | `FamilyPlusPaywallViewModelTest`, `RevenueCatEntitlementRepositoryTest` |
| Missing annual package | Automated pass | `FamilyPlusPaywallViewModelTest` default-selection coverage |
| Missing lifetime package | Automated pass | `RevenueCatMappersTest`, `RevenueCatEntitlementRepositoryTest` |
| Malformed package metadata | Automated pass | `RevenueCatMappersTest` |
| Purchase cancellation | Automated pass | `FamilyPlusPaywallViewModelTest` |
| Purchase pending | Automated pass | `FamilyPlusPaywallViewModelTest` |
| Already-owned purchase | Automated pass | `RevenueCatMappersTest`, `FamilyPlusPaywallViewModelTest` |
| Network loss during purchase | Automated pass | `RevenueCatMappersTest` error mapping + paywall failure path |
| Store unavailable | Automated pass | `RevenueCatMappersTest` error mapping + paywall failure path |
| Purchase succeeds but entitlement refresh is delayed | Automated pass | `RevenueCatEntitlementRepositoryTest` reconciles confirmed access and later push updates |
| Restore finds nothing | Automated pass | `FamilyPlusPaywallViewModelTest` |
| Restore activates Family Plus | Automated pass | `FamilyPlusPaywallViewModelTest` |
| Expired subscription | Automated pass | `RevenueCatEntitlementRepositoryTest` |
| Refunded purchase | Automated pass | `RevenueCatEntitlementRepositoryTest` customer-info push reconciliation |
| Lifetime purchase | Automated pass | `RevenueCatEntitlementRepositoryTest`, `RevenueCatMappersTest` |
| Offline cached premium state | Automated pass | `RevenueCatEntitlementRepositoryTest` |
| Anonymous RevenueCat identity after local-data reset | Automated pass | `RevenueCatAnalyticsLinkerTest`, `DebugTelemetryViewModelTest` |
| App restart during purchase | Automated pass | `RevenueCatEntitlementRepositoryTest` duplicate purchase coalescing |
| Duplicate purchase taps | Automated pass | `FamilyPlusPaywallViewModelTest`, `RevenueCatEntitlementRepositoryTest` |
| Customer Center unavailable | Automated pass | `FamilyPlusManagementViewModelTest` |

## Manual RevenueCat Test Store checklist

| Platform | Precondition | Steps | Expected result | Status |
| --- | --- | --- | --- | --- |
| Android | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Open the paywall, tap Buy, then cancel from the store sheet | Purchase is treated as cancellation, not an error; Family Plus remains locked | Pending RevenueCat Test Store verification |
| Android | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Start a purchase and disconnect network before completion | The purchase fails safely, free functionality remains usable, and no entitlement unlocks from a partial flow | Pending RevenueCat Test Store verification |
| Android | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Attempt to buy a product that is already owned | The app reports already-owned state and does not unlock twice | Pending RevenueCat Test Store verification |
| Android | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Restore purchases when no active purchase exists | Restore completes without unlocking Family Plus | Pending RevenueCat Test Store verification |
| Android | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Restore after a valid sandbox purchase | Restore reactivates Family Plus from RevenueCat customer info | Pending RevenueCat Test Store verification |
| Android | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Start a purchase, force-stop the app, then relaunch | A completed purchase is reconciled from provider state; a cancelled or pending purchase remains non-premium | Pending RevenueCat Test Store verification |
| iOS | RevenueCat Test Store is configured and the app is signed in to a sandbox/test account | Repeat the purchase and restore checks above | Behavior matches Android, with StoreKit purchase sheet semantics | Pending RevenueCat Test Store verification |

## Notes

- The code path does not grant Family Plus on button completion alone; it waits for provider-confirmed access.
- No code changes were required for the entitlement source-of-truth rule.
- This doc deliberately separates automated test coverage from RevenueCat Test Store checks that still need manual execution on each platform.
