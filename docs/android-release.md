# Togetherly Android release behavior

This documents the current Android release posture and the verification that backs it.

## Release configuration

- Application ID: `com.togetherly.app`
- Version name source: `androidApp/build.gradle.kts` `defaultConfig.versionName`
- Version code source: `androidApp/build.gradle.kts` `defaultConfig.versionCode`
- Minimum SDK: 26
- Target SDK: 36
- Application label: `@string/app_name`
- Application icon: `@mipmap/ic_launcher`
- Round icon: `@mipmap/ic_launcher_round`
- Cleartext traffic: explicitly disabled with `android:usesCleartextTraffic="false"`
- Backup posture: disabled with `android:allowBackup="false"`
- Backup XML: `@xml/backup_rules`
- Android 12+ extraction rules: `@xml/data_extraction_rules`
- Release minification: enabled
- Release resource shrinking: enabled
- Release ProGuard file: `androidApp/proguard-rules.pro`
- Release signing credentials: not committed; the repository currently builds release artifacts without a production signing config
- Debug-only tooling: `compose.uiTooling` remains `debugImplementation` only

## Release safety notes

- Togetherly does not use a custom `networkSecurityConfig`; the app is HTTPS-only and release traffic is not allowed to use cleartext.
- RevenueCat, PostHog, and Sentry configuration values are read from gitignored local developer configuration and may be blank in local builds.
- The release build stays usable in free mode if RevenueCat is unavailable or misconfigured; the app does not depend on local debug files.
- The debug telemetry screen and other debug-only entry points are gated by `BuildConfig.DEBUG` and are not reachable in release.
- R8 keep rules preserve Kotlin serialization metadata and Room-annotated application types.

## Static verification

The module includes a release/posture check:

```bash
./gradlew :androidApp:verifyAndroidReleaseConfiguration
```

It verifies:

- release minification is enabled
- resource shrinking is enabled
- backup remains disabled
- cleartext traffic remains disabled
- app label and icons remain declared
- the ProGuard file carries serialization and Room preservation rules

Backup policy verification remains separate:

```bash
./gradlew :androidApp:verifyAndroidBackupPolicy
```

## Practical build checks

Run these before shipping:

```bash
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease
./gradlew :androidApp:lintDebug
```

If you want an extra release sanity check after minification, run the release unit/build slice the repo already uses in CI-style validation:

```bash
./gradlew :shared:testAndroidHostTest :shared:compileAndroidDeviceTest :androidApp:assembleRelease
```

## Manual review checklist

- Confirm the merged release manifest contains `allowBackup="false"` and `usesCleartextTraffic="false"`.
- Confirm `debugImplementation` dependencies are not pulled into release.
- Confirm the release APK starts, loads the bundled catalogue, and reaches the home flow.
- Confirm debug telemetry screens are absent from the release UI.
- Confirm no local signing credentials are required for release compilation.
- Confirm the app still opens the paywall and local data flows after minification.

## Trade-off

Release APKs are now harder to introspect, but Togetherly’s user data remains protected by backup disablement and the build becomes closer to the actual shipped posture.
