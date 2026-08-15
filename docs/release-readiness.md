# Release readiness baseline

Audit date: 2026-07-29 (Europe/Skopje)  
Audited checkout: `fe36eb5` on `chore/step-15-production-hardening`  
Host: Apple silicon macOS, Java 21.0.9, Xcode 26.6 / iOS Simulator SDK 26.5

This is a read-only assessment of production code. The only repository change made by the audit is
this document. Build outputs and reports are ignored artifacts. Status values mean:

- **Ready** — present and the applicable verification passed.
- **Needs work** — present, but a repository-owned gap or warning remains.
- **Blocked externally** — requires credentials, hardware, a service, or an environment not
  available to this checkout.
- **Not applicable** — the capability/tool is not configured or has no runnable work.

The audit is intentionally tied to the commit above. During the audit, PR #7 was merged remotely to
`main` to update Gradle/AGP/compileSdk, but the audited checkout was not changed. Dependency PRs #1
and #3 also remained open.

## Baseline summary

| Area | Status | Baseline |
|---|---|---|
| Gradle structure | **Ready** | Root project `Togetherly`; modules `:androidApp` and `:shared`; Gradle 9.1.0; daemon JDK 21; compilation toolchain JDK 17. |
| KMP targets | **Ready** | `:shared` targets Android, `iosArm64`, `iosSimulatorArm64`, plus common/native/apple/iOS hierarchical source sets. It builds a static `Shared.framework`. No JVM desktop, web, watchOS, tvOS, macOS, or x86_64 iOS target is declared. |
| Android variants | **Ready** | Application variants are `debug` and `release`; there are no product flavors. `debugAndroidTest` and `debugUnitTest` components exist. The shared Android target has `androidMain`, `androidHostTest`, and `androidDeviceTest`. |
| Android platform levels | **Ready** | Audited checkout: minSdk 26, targetSdk 36, compileSdk 36. The remote toolchain PR #7 subsequently changed compileSdk to 37 without changing min/target. |
| Android debug build | **Ready** | `androidApp-debug.apk` assembled successfully. |
| Android release compilation | **Needs work** | `androidApp-release-unsigned.apk` assembled successfully with no signing config. R8/minification is disabled, release signing is absent, and four native libraries could not be stripped. |
| iOS target and host setup | **Ready** | Xcode project `iosApp.xcodeproj`, shared scheme `iosApp`, SwiftUI host, Sentry Cocoa 8.58.4 via Swift Package Manager, and a `Compile Kotlin Framework` build phase invoking `:shared:embedAndSignAppleFrameworkForXcode`. Debug and Release simulator host builds succeeded without signing. |
| iOS platform level | **Ready** | Deployment target is iOS 16.0 for iPhone and iPad. It now reflects the lowest practical floor supported by the current dependency and API set, and the app builds, runs, and archives at that level. |
| iOS distribution signing | **Blocked externally** | `TEAM_ID` is blank and no distribution certificate, provisioning profile, or App Store Connect credentials are present. This is correct for a public checkout but blocks archive/upload validation. |
| Common and Android host tests | **Ready** | `:shared:testAndroidHostTest` passed. This runs common tests plus Android host tests and includes real bundled-catalogue validation. |
| Android app unit tests | **Not applicable** | `:androidApp:testDebugUnitTest` succeeded with `NO-SOURCE`; no app-module unit tests exist. Shared host tests contain the JVM-side coverage. |
| Android device tests | **Blocked externally** | 54 Kotlin source/support files and 254 `@Test` occurrences exist under `androidDeviceTest`, but `adb` is not installed on this shell and no emulator/device execution was available. |
| iOS tests | **Needs work** | The test binary links. Execution ran 1,295 tests and then aborted on the known `IosReminderSchedulerTest` app-bundle limitation; CI currently allows this step to fail. |
| Static analysis | **Ready** | Android Lint is configured and `:androidApp:lintDebug` passed, producing HTML/XML/text reports. GitHub-managed CodeQL and dependency review are also active. |
| Formatting | **Not applicable** | No ktlint, Detekt formatting, Spotless, or other formatting plugin/task is configured. Therefore there is no formatting check to run or enforce in CI. |
| Catalogue validation | **Ready** | No dedicated Gradle task exists. `BundledCatalogueContentTest` and loader/validator tests run inside `:shared:testAndroidHostTest`; the real catalogue (45 quests, 6 packs) passed. |
| Local runtime configuration | **Needs work** | `local.properties` exists with `sdk.dir` and RevenueCat Android key configured. PostHog key/host and Sentry DSN are absent. All three iOS `*.local.xcconfig` files are missing. Documented no-op/free fallbacks allow builds. |
| Release credentials and telemetry upload | **Blocked externally** | Android keystore/signing values, Apple distribution credentials, and Sentry upload values (`SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT`) are intentionally absent. No mapping/dSYM upload pipeline exists. |
| TODO/FIXME markers | **Needs work** | One production TODO remains: placeholder paywall legal URLs in `FamilyPlusPaywallViewModel.kt`. The other search hit is a test list of forbidden placeholder words, not unresolved work. |
| Dependency state | **Needs work** | The audited catalog uses prerelease Lifecycle `2.11.0-beta01` and Material3 `1.11.0-alpha07`. PR #3 updates Lifecycle to 2.11.0 and PR #1 updates Material3 to `1.12.0-alpha03`; Linux/security checks pass but macOS checks were queued. PR #7 updated Gradle/AGP/compileSdk remotely after this snapshot. |
| CI coverage | **Needs work** | Linux and Apple compile gates, wrapper validation, Lint, host tests, metadata, debug build, CodeQL, and dependency review exist. Release builds, Android device tests, reliable iOS test execution, formatting, artifact signing, and store/archive validation are not gating. |

## Repository and target inventory

### Gradle modules

- `:androidApp` — Android application, namespace/application ID `com.togetherly.app`, Compose UI,
  versionCode `1`, versionName `1.0`.
- `:shared` — Kotlin Multiplatform Android library and static Apple framework, namespace
  `com.togetherly.app.shared`. It owns common UI/domain/data code, Room/KSP, resources, and most
  tests.

The audited catalog uses Kotlin 2.4.10, Compose Multiplatform 1.11.1, AGP 9.0.1, and Gradle 9.1.0.
Configuration cache and build cache are enabled.

### Android variants and release output

- Build types: `debug`, `release`.
- No flavors or flavor dimensions.
- Release minification: disabled (`isMinifyEnabled = false`).
- Release signing: no explicit signing configuration; `assembleRelease` emits
  `androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk`.
- Primary release command: `./gradlew :androidApp:assembleRelease --stacktrace --no-daemon`.
- App bundle command available but not exercised separately: `./gradlew :androidApp:bundleRelease`.

### iOS and Xcode

- Kotlin targets: physical-device ARM64 and simulator ARM64 only.
- Framework: static, base name `Shared`, Debug and Release link tasks available.
- Host: `iosApp/iosApp.xcodeproj`, shared `iosApp` scheme, Swift 5 mode, iPhone/iPad families,
  bundle ID derived from `com.togetherly.app$(TEAM_ID)`.
- Xcode calls Gradle through the `Compile Kotlin Framework` build phase.
- Sentry Cocoa 8.58.4 is locked through Swift Package Manager.
- Unsigned simulator release command:

```bash
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' -configuration Release \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY=
```

A production device archive/export command cannot be completed until an Apple team and distribution
credentials exist. No archive/export automation or `ExportOptions.plist` is committed.

## Test inventory

Counts include fixtures/support Kotlin files as well as test classes; `@Test` counts are a useful
upper-bound inventory because parameterized/platform compilation can change the runtime total.

| Source set | Kotlin files | `@Test` occurrences | Execution |
|---|---:|---:|---|
| `commonTest` | 220 | 1,284 | Passed through Android host test task; also compiled into iOS tests. |
| `androidHostTest` | 3 | 11 | Passed. |
| `androidDeviceTest` | 54 | 254 | Not run: Android device tooling unavailable. Covers Room/DAO, repositories, media, navigation/UI, DI, notifications, and telemetry integrations. |
| `iosTest` | 8 | 29 | Compiled; runtime suite aborts on the known notification-center test after reporting 1,295 tests / 1 failure. |
| `androidApp` unit tests | 0 | 0 | `NO-SOURCE`. |

## Static analysis, formatting, and dependency controls

- Android Lint is the only local static-analysis tool. No baseline file or custom lint policy was
  found; the Debug report passed.
- Formatting is convention-only (`kotlin.code.style=official`), with no executable formatter or
  CI check.
- GitHub CodeQL uses default setup rather than a checked-in workflow. Its Kotlin coverage is JVM
  oriented and does not cover iOS-only actual implementations.
- Dependency Review runs on pull requests and fails newly introduced high/critical
  vulnerabilities. Dependabot checks Gradle and GitHub Actions weekly.
- `:shared:printResolvedRuntimeDependencies` succeeds and provides a manual license-catalogue
  cross-check; no automated license or dependency-version validation plugin exists.

## GitHub Actions checks

| Check | Trigger and scope | Status |
|---|---|---|
| `Verify (Linux)` | PR/push to `main`, manual dispatch. Wrapper validation, Lint Debug, shared Android host tests, common metadata, Android Debug assembly. | **Ready** |
| `Detect Apple-relevant changes` | Always runs; gates expensive Apple work based on changed paths. | **Ready** |
| `Verify (macOS)` | Relevant PR/push/manual dispatch on `macos-15-arm64`, Xcode 16.4. Compiles both iOS targets, links framework/test binary, non-blockingly runs simulator tests, and builds Debug simulator host without signing. | **Needs work** — test execution is non-blocking and runner availability currently leaves PRs queued. |
| `Scan dependency changes` | PRs to `main`; fails new high/critical dependency vulnerabilities. | **Ready** |
| `Analyze (actions)` / `CodeQL` | GitHub-managed default setup. | **Ready**, with the platform coverage limitation above. |

The branch ruleset requires up-to-date `Verify (Linux)` and `Verify (macOS)` results, squash-only
merges, resolved conversations, and a pull request. At audit time both open dependency PRs had green
Linux, CodeQL, and dependency-review checks but queued macOS checks.

## Exact verification commands and results

| Exact command | Result | Classification |
|---|---|---|
| `./gradlew projects tasks --all --console=plain` | Passed; enumerated two modules and all tasks. | **Ready** |
| `./gradlew :shared:testAndroidHostTest --stacktrace --no-daemon` | Passed in 6s; common + Android host tests, including catalogue checks. | **Ready** |
| `./gradlew :androidApp:testDebugUnitTest --stacktrace --no-daemon` | Passed in 10s with `NO-SOURCE`. | **Not applicable** |
| `./gradlew :androidApp:lintDebug --stacktrace --no-daemon` | Passed in 6s; reports written under `androidApp/build/reports/`. | **Ready** |
| `./gradlew :shared:compileCommonMainKotlinMetadata --stacktrace --no-daemon` | Passed in 27s with five compiler warnings. | **Needs work** |
| `./gradlew :androidApp:assembleDebug --stacktrace --no-daemon` | Passed in 11s; debug APK produced. | **Ready** |
| `./gradlew :androidApp:assembleRelease --stacktrace --no-daemon` | Passed in 1m 4s; unsigned release APK produced; four native libraries were not stripped. | **Needs work** |
| `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64 --stacktrace --no-daemon` | Passed in 17s. | **Ready** |
| `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --stacktrace --no-daemon` | Passed in 46s; warned that `Shared` has no explicit bundle ID. | **Needs work** |
| `./gradlew :shared:linkDebugTestIosSimulatorArm64 --stacktrace --no-daemon` | Passed in 7s. | **Ready** |
| `./gradlew :shared:iosSimulatorArm64Test --stacktrace --no-daemon` | Failed in 9s: 1,295 tests, 1 failure; process aborted in `IosReminderSchedulerTest` because the bare test binary has no app bundle. | **Needs work** |
| `./gradlew :shared:checkXcodeProjectConfiguration --stacktrace --no-daemon` | Passed in 7s. | **Ready** |
| `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Debug CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY=` | `BUILD SUCCEEDED`; noted the Kotlin script phase always runs. | **Ready** |
| `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Release CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY=` | `BUILD SUCCEEDED`; Kotlin release framework took 6m 43s and emitted extensive missing module-cache/debug-symbol warnings from prebuilt RevenueCat/RevenueCatUI artifacts. | **Needs work** |
| `./gradlew :shared:printResolvedRuntimeDependencies --stacktrace --no-daemon` | Passed in 8s; resolved runtime coordinates printed for manual license review. | **Ready** |
| Formatting check | No task exists. | **Not applicable** |
| Dedicated catalogue validation | No standalone task exists; validation passed within `:shared:testAndroidHostTest`. | **Ready** |
| Android connected/device tests | Not run: `adb` is unavailable in this shell and no device/emulator was established. | **Blocked externally** |

### Warnings captured

1. `IdGenerator` and `TogetherlyDatabase`: expect/actual classes are still a Beta feature.
2. `JourneySummary` and `FamilyAccess`: private constructors are exposed through generated
   `copy()`; this becomes an error in Kotlin language version 2.5.
3. `DataManagementRoute`: Compose `BackHandler` API is deprecated in favor of
   `NavigationEventHandler`.
4. Kotlin/Native framework link cannot infer an explicit bundle ID and falls back to `Shared`.
5. Android release packaging cannot strip `libandroidx.graphics.path.so`, `libsentry-android.so`,
   `libsentry.so`, and `libsqliteJni.so`.
6. Xcode Release succeeds but emits many missing `.pcm` module-cache and symbol warnings from the
   prebuilt RevenueCat/RevenueCatUI static artifacts; debug information is degraded.
7. Xcode notes that `Compile Kotlin Framework` runs on every build because dependency-based
   execution is disabled.

## Missing local and release configuration

| Configuration | State | Status |
|---|---|---|
| Android SDK path | Present in ignored `local.properties`. | **Ready** |
| Android RevenueCat public SDK key | Present locally (value not inspected or recorded). | **Ready** |
| Android PostHog project key/host | Absent from local file. Builds use documented no-op/default behavior. | **Needs work** |
| Android Sentry DSN | Absent from local file. Builds use no-op diagnostics. | **Needs work** |
| iOS RevenueCat/PostHog/Sentry local xcconfigs | All three files absent; tracked `.example` templates exist. | **Needs work** |
| Apple team ID and distribution material | Blank/absent. | **Blocked externally** |
| Android release keystore and signing values | Absent; no Gradle release signing config. | **Blocked externally** |
| Sentry mapping/dSYM upload credentials | Absent and no upload jobs exist. | **Blocked externally** |
| Published privacy policy and terms URLs | Placeholder production constants remain. | **Blocked externally** for the final URLs; repository wiring still **Needs work** afterward. |

## TODO/FIXME audit

The exact production marker is:

```text
shared/src/commonMain/kotlin/com/togetherly/feature/paywall/presentation/FamilyPlusPaywallViewModel.kt:252
TODO(revenuecat-setup): placeholder legal URLs — replace with Togetherly's real, published ...
```

The search also matches `BundledCatalogueContentTest.kt:177`, where `todo`, `xxx`, and `fixme` are
intentional strings in a test that rejects placeholder catalogue content. It is not unresolved work.

## Ordered hardening backlog

1. **Replace and verify the paywall legal URLs.** Publish the privacy policy and terms, configure
   real URLs, and test them on both platforms. This is the clearest user-facing release blocker.
2. **Establish release identity and signing outside the repository.** Create/secure the Android
   keystore and Apple distribution/App Store Connect credentials; wire reproducible signed Android
   bundles and iOS archives without committing secrets.
3. **Add signed release pipelines and store-grade validation.** Build `bundleRelease`, archive and
   export iOS, validate artifacts, and upload ProGuard mappings/dSYMs to Sentry using CI secrets.
4. **Make iOS tests reliable and gating.** Run notification-center behavior in an app-hosted test
   target or inject the platform dependency so the bare Kotlin/Native process no longer aborts;
   remove `continue-on-error` only after it is reliable.
5. **Run and gate Android device tests.** Provision a managed emulator/device in CI and execute the
   254 instrumentation tests, especially Room migration/DAO, private-media, notification, DI, and
   Compose UI coverage.
6. **Resolve Kotlin forward-compatibility warnings.** Fix `JourneySummary`/`FamilyAccess` copy
   visibility before language 2.5, migrate deprecated `BackHandler`, and make an explicit policy for
   expect/actual Beta warnings.
7. **Investigate release native-symbol quality.** Determine why Android native libraries are not
   stripped and whether symbol files are retained/uploaded; investigate RevenueCat/RevenueCatUI
   missing module-cache warnings and their effect on iOS crash symbolication.
8. **Add an explicit Kotlin framework bundle ID.** Remove the fallback `Shared` identity warning and
   verify Debug/Release framework metadata.
9. **Keep the iOS minimum target under review.** The current floor is iOS 16.0. Raise it only if
   future APIs or dependencies require it, and test supported OS versions whenever it changes.
10. **Complete production telemetry and purchase configuration.** Populate platform-local values,
    verify RevenueCat entitlement/product setup, PostHog routing/consent, and Sentry event delivery
    in release-like builds.
11. **Finish dependency updates and formalize dependency reporting.** Land the toolchain/Lifecycle/
    Material3 updates after full Apple checks, review prerelease production dependencies, and add an
    automated outdated/version report if desired.
12. **Add formatting and broader static-analysis gates.** Introduce one consistent formatter and
    consider Detekt or equivalent KMP-aware analysis; keep Android Lint and CodeQL.
13. **Expand release CI scope.** Gate Android Release and iOS Release simulator compilation (not only
    Debug), and consider performance/binary-size budgets and smoke installation tests.
14. **Optimize the Xcode Kotlin build phase.** Declare inputs/outputs or enable dependency analysis
    once correctness is proven, reducing the current repeated framework build cost.
15. **Add direct `androidApp` host tests where appropriate.** The app module currently has no unit
    test source; retain shared ownership for business logic but cover app-specific bootstrap and
    manifest/build-config behavior where valuable.
