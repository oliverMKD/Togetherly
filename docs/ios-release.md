# Togetherly iOS release review

This documents the current iOS/Xcode release posture for the `iosApp` target as observed in the repository and verified with local builds.

## Current configuration

- Bundle identifier source: `iosApp/Configuration/Config.xcconfig`
  - `PRODUCT_BUNDLE_IDENTIFIER = com.togetherly.app$(TEAM_ID)`
  - `TEAM_ID` is blank in the committed config, so the effective bundle ID is `com.togetherly.app`.
- Version source: `iosApp/Configuration/Config.xcconfig`
  - `MARKETING_VERSION = 1.0`
  - `CURRENT_PROJECT_VERSION = 1`
- Minimum iOS version: `IPHONEOS_DEPLOYMENT_TARGET = 16.0`
- Display name: derived from `PRODUCT_NAME = Togetherly`
- App icons: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`
- Release/debug separation:
  - `iosApp/iosApp/iOSApp.swift` uses `#if DEBUG` to toggle debug initialization.
  - The release build configuration does not define the `DEBUG` Swift compilation condition.
- Framework linking:
  - The app target links the Sentry Swift Package.
  - The shared KMP framework is built and linked during the Xcode build phase.
  - The shared framework minimum OS version is set to 16.0 so it matches the app target.

## Notification boundary

- The reminder stack now routes through an internal `IosNotificationCenterAdapter`.
- Native unit tests use a deterministic fake adapter and do not touch `UNUserNotificationCenter.currentNotificationCenter()`.
- Production adapter code owns all UserNotifications framework calls; Apple framework types stay inside that boundary.
- Real notification delivery still requires a real device or simulator with permission granted and a live clock.

## Privacy usage descriptions

The app currently uses:

- photo picking via `PHPickerViewController`
- microphone recording for voice memories

The effective built plist contains:

- `NSMicrophoneUsageDescription`
- `NSPhotoLibraryUsageDescription`

It does not contain a camera usage string, which matches the current codebase because the app does not use direct camera capture.

Notifications are local notifications only. There is no iOS usage-description key for that capability, and the target does not need a push-notification entitlement for the current feature set.

## Storage and security posture

- Private media is stored under Application Support, not Documents.
- Application Support and private media directories are marked with file protection and excluded from backup in the shared iOS platform code.
- RevenueCat, PostHog, and Sentry keys are read from `Info.plist` placeholders populated by local xcconfig overrides.
- The committed plist does not contain real service keys.

## Build and archive verification

Command results:

- `xcodebuild -list -project iosApp/iosApp.xcodeproj`
  - Passed.
  - Scheme: `iosApp`
  - No native XCTest target is attached to the scheme.

- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -derivedDataPath /private/tmp/codex-ios-dd-sim CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build`
  - Passed.
  - Verified simulator compilation and framework linking at the 16.0 deployment target.

- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -sdk iphoneos -derivedDataPath /private/tmp/codex-ios-dd-release CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build`
  - Passed.
  - Verified unsigned release compilation for device SDK at the 16.0 deployment target.

- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -sdk iphoneos -archivePath /private/tmp/Togetherly-ios-release.xcarchive CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO archive`
  - Passed.
  - Archive validation completed locally without production signing.

- `GRADLE_USER_HOME=/private/tmp/codex-gradle ./gradlew :shared:iosSimulatorArm64Test --stacktrace --no-daemon`
  - Passed.
  - `JourneyUiMapperTest` now normalizes iOS's narrow no-break space in the localized AM/PM separator.
  - `IosReminderSchedulerTest` now uses the fake notification-center adapter and no longer depends on the native notification center inside unit tests.

## Apple account / App Store Connect requirements

The following still require Apple-managed configuration outside the repository:

- Distribution signing certificates and provisioning profiles
- App Store Connect archive upload / TestFlight distribution
- Final App Store metadata and release submission

If the app later adds push notifications or any new Apple entitlement, those capabilities will also require Apple Developer account setup.

## Notes

- The effective built plist shows empty values for `REVENUECAT_API_KEY`, `POSTHOG_PROJECT_KEY`, `POSTHOG_HOST`, and `SENTRY_DSN` in a fresh checkout.
- The release build succeeded with unsigned local compilation; that is enough for CI-style verification, but it is not a store-signable release artifact.
- Final iOS deployment target: 16.0.
- Reason: the app's current codebase does not require 18.2-only APIs, and the current Compose/Room/iOS stack remains practical on 16.0. Lowering the floor from 18.2 to 16.0 broadens device coverage without forcing app behavior changes.
