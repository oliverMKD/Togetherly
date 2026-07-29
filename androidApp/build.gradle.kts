import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

/**
 * Read only from the gitignored `local.properties` (never committed — see
 * `local.properties.example` and `docs/revenuecat-setup.md`), never hardcoded here. Missing the
 * file/property is not a build failure: it resolves to an empty string, and
 * `RevenueCatConfigurator` (shared module) is what decides what an empty key means for a debug vs.
 * release build — this build script's only job is to get *some* string value from local
 * developer configuration into `BuildConfig`, never to judge whether it's present.
 */
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val revenueCatAndroidApiKey: String = localProperties.getProperty("revenueCat.androidApiKey", "")

/** See `local.properties.example` and `docs/analytics-setup.md` — same "read only from gitignored local.properties" contract as [revenueCatAndroidApiKey] above. */
val posthogProjectKey: String = localProperties.getProperty("posthog.projectKey", "")
val posthogHost: String = localProperties.getProperty("posthog.host", "")

/** See `local.properties.example` and `docs/sentry-setup.md` — same "read only from gitignored local.properties" contract as [revenueCatAndroidApiKey] above. A DSN, never a Sentry auth token. */
val sentryDsn: String = localProperties.getProperty("sentry.dsn", "")

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.togetherly.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.togetherly.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatAndroidApiKey\"")
        buildConfigField("String", "POSTHOG_PROJECT_KEY", "\"$posthogProjectKey\"")
        buildConfigField("String", "POSTHOG_HOST", "\"$posthogHost\"")
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}