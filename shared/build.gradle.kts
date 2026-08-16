import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom)
    alias(libs.plugins.sentryKotlinMultiplatform)
}

/**
 * RevenueCat's KMP artifacts wrap real Swift-compiled static libraries (Step 11). Kotlin/Native's
 * own test-binary linker invocation doesn't automatically search Xcode's Swift compatibility
 * library path the way Xcode's own build system does, which otherwise fails
 * `linkDebugTestIosSimulatorArm64`/`linkDebugTestIosArm64` with undefined `swiftCompatibility56`/
 * `swiftCompatibilityConcurrency` symbols — the main app framework link is unaffected (it links
 * fine without this). Resolved via `xcode-select -p` rather than a hardcoded path, so this works
 * regardless of where a given machine's Xcode.app lives.
 *
 * Only ever attempted on macOS: `xcode-select` doesn't exist on Linux/Windows at all, and a
 * `providers.exec {}` call for a genuinely missing executable escapes `runCatching` once
 * Configuration Cache is enabled (`org.gradle.configuration-cache=true`, this project's own
 * `gradle.properties`) — the underlying `ValueSource` is obtained through Gradle's own internal
 * machinery, outside this script's `runCatching` block's dynamic scope, so the exception surfaces
 * as a build failure instead of being caught here. Confirmed by CI (Step 14.5.4): Linux runners
 * failed configuring this module entirely until the OS check below was added. `null` (not macOS,
 * or `xcode-select` failed/found nothing) simply skips adding the search path.
 */
val xcodeSwiftLibraryPath: String? = if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
    runCatching {
        providers.exec { commandLine("xcode-select", "-p") }.standardOutput.asText.get().trim()
    }.getOrNull()?.takeIf { it.isNotBlank() }
} else {
    null
}

kotlin {
    jvmToolchain(17)

    listOf(
        iosArm64() to "iphoneos",
        iosSimulatorArm64() to "iphonesimulator",
    ).forEach { (iosTarget, swiftPlatformDirectory) ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            freeCompilerArgs += listOf(
                "-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=16.0;osVersionMin.ios_arm64=16.0",
            )
        }
        if (xcodeSwiftLibraryPath != null) {
            iosTarget.binaries.getTest("DEBUG").linkerOpts(
                "-L$xcodeSwiftLibraryPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$swiftPlatformDirectory",
            )
        }
    }

    android {
       namespace = "com.togetherly.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiBackhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            // RevenueCat KMP SDK (Step 11) — core is the SDK/offerings/entitlements API used by
            // data.purchase; -ui provides the Paywall/Customer Center composables used only at
            // the feature.paywall/feature.family UI boundary. Both live in commonMain because the
            // KMP artifact itself is what supplies the androidMain/iosMain actuals internally —
            // this module never needs its own android/iosMain RevenueCat dependency.
            implementation(libs.purchases.kmp.core)
            implementation(libs.purchases.kmp.ui)

            // PostHog KMP SDK (Step 14.2) — a thin common wrapper delegating to the official
            // native Android/iOS SDKs; the published artifact already bundles iOS interop (Swift
            // Package Manager, resolved at PostHog's own publish time), so this module needs no
            // extra iOS-side SPM/CocoaPods setup of its own, the same "already-linked KMP
            // artifact" shape purchases-kmp already established for RevenueCat.
            implementation(libs.posthog.kmp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.compose.uiTest)
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.room.testing)
                implementation(libs.androidx.compose.uiTestManifest)
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

/**
 * Prints every resolved runtime dependency's `group:name:version` coordinate — see
 * `docs/open-source-licenses.md` for how this is used to cross-check
 * `com.togetherly.feature.family.model.OPEN_SOURCE_LICENSES`, the hand-curated list actually shown
 * in-app. `androidRuntimeClasspath` is used as the representative resolved classpath (Android's
 * runtime dependency set matches commonMain's, since none of this module's dependencies are
 * android-only) — this project deliberately doesn't add a third-party license-generation plugin
 * (see that doc for why), so this task's output is meant to be read by a developer, not consumed
 * automatically.
 */
tasks.register("printResolvedRuntimeDependencies") {
    group = "help"
    description = "Prints resolved runtime dependency coordinates for cross-checking docs/open-source-licenses.md."
    val resolvedComponents = configurations.named("androidRuntimeClasspath").map { it.incoming.resolutionResult.allComponents }
    doLast {
        resolvedComponents.get()
            .mapNotNull { it.moduleVersion }
            .map { "${it.group}:${it.name}:${it.version}" }
            .distinct()
            .sorted()
            .forEach { println(it) }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}
