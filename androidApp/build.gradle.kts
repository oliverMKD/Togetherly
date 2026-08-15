import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
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
            isMinifyEnabled = true
            isShrinkResources = true
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

val verifyAndroidBackupPolicy = tasks.register("verifyAndroidBackupPolicy") {
    group = "verification"
    description = "Verifies the Android backup posture is disabled and the declared XML rules exclude all user data storage domains."

    val manifestFile = layout.projectDirectory.file("src/main/AndroidManifest.xml")
    val fullBackupRulesFile = layout.projectDirectory.file("src/main/res/xml/backup_rules.xml")
    val dataExtractionRulesFile = layout.projectDirectory.file("src/main/res/xml/data_extraction_rules.xml")

    inputs.files(manifestFile, fullBackupRulesFile, dataExtractionRulesFile)

    doLast {
        fun assertExcludedRules(file: File, expectedRules: Set<Pair<String, String>>) {
            val pattern = Regex("""<exclude\s+domain="([^"]+)"\s+path="([^"]+)"\s*/?>""")
            val text = file.readText()
            val actualRules = buildSet {
                pattern.findAll(text).forEach { match ->
                    add(match.groupValues[1] to match.groupValues[2])
                }
            }
            check(actualRules == expectedRules) {
                "Unexpected backup exclusions in ${file.name}: expected=$expectedRules actual=$actualRules"
            }
            check(!text.contains("<include")) {
                "Backup rules in ${file.name} must not include any data."
            }
        }

        val manifestText = manifestFile.asFile.readText()
        check(manifestText.contains("""android:allowBackup="false"""")) {
            "android:allowBackup must be false for release builds."
        }
        check(manifestText.contains("""android:fullBackupContent="@xml/backup_rules"""")) {
            "android:fullBackupContent must point at @xml/backup_rules."
        }
        check(manifestText.contains("""android:dataExtractionRules="@xml/data_extraction_rules"""")) {
            "android:dataExtractionRules must point at @xml/data_extraction_rules."
        }

        val expectedRules = setOf(
            "root" to "./",
            "file" to "./",
            "database" to "togetherly.db",
            "database" to "togetherly.db-wal",
            "database" to "togetherly.db-shm",
            "external" to "./",
            "device_root" to "./",
            "device_file" to "./",
            "device_database" to "togetherly.db",
            "device_database" to "togetherly.db-wal",
            "device_database" to "togetherly.db-shm",
        )
        assertExcludedRules(fullBackupRulesFile.asFile, expectedRules)

        val dataExtractionText = dataExtractionRulesFile.asFile.readText()
        fun excludedRules(blockName: String): Set<Pair<String, String>> {
            val blockPattern = Regex("""<$blockName>(.*?)</$blockName>""", setOf(RegexOption.DOT_MATCHES_ALL))
            val block = blockPattern.find(dataExtractionText)?.groupValues?.get(1)
                ?: error("Could not find <$blockName> in ${dataExtractionRulesFile.asFile.name}")
            return buildSet {
                Regex("""<exclude\s+domain="([^"]+)"\s+path="([^"]+)"\s*/?>""")
                    .findAll(block)
                    .forEach { match -> add(match.groupValues[1] to match.groupValues[2]) }
            }
        }
        check(excludedRules("cloud-backup") == expectedRules) {
            "Unexpected cloud-backup exclusions in ${dataExtractionRulesFile.asFile.name}"
        }
        check(excludedRules("device-transfer") == expectedRules) {
            "Unexpected device-transfer exclusions in ${dataExtractionRulesFile.asFile.name}"
        }
    }
}

val verifyAndroidReleaseConfiguration = tasks.register("verifyAndroidReleaseConfiguration") {
    group = "verification"
    description = "Verifies the Android release build posture: minification, shrinking, manifest flags, and explicit cleartext denial."

    val manifestFile = layout.projectDirectory.file("src/main/AndroidManifest.xml")
    val proguardRulesFile = layout.projectDirectory.file("proguard-rules.pro")
    val releaseBuildType = android.buildTypes.getByName("release")
    val releaseMinifyEnabled = releaseBuildType.isMinifyEnabled
    val releaseShrinkResources = releaseBuildType.isShrinkResources

    inputs.files(manifestFile, proguardRulesFile)

    doLast {
        check(releaseMinifyEnabled) {
            "Release builds must enable R8/minification."
        }
        check(releaseShrinkResources) {
            "Release builds must enable resource shrinking."
        }

        val manifestText = manifestFile.asFile.readText()
        check(manifestText.contains("""android:allowBackup="false"""")) {
            "Release builds must disable backup."
        }
        check(manifestText.contains("""android:usesCleartextTraffic="false"""")) {
            "android:usesCleartextTraffic must be explicitly false in release."
        }
        check(manifestText.contains("""android:icon="@mipmap/ic_launcher"""")) {
            "Release builds must keep the app icon."
        }
        check(manifestText.contains("""android:roundIcon="@mipmap/ic_launcher_round"""")) {
            "Release builds must keep the round icon."
        }
        check(manifestText.contains("""android:label="@string/app_name"""")) {
            "Release builds must keep the application label."
        }

        val proguardRulesText = proguardRulesFile.asFile.readText()
        check(proguardRulesText.contains("kotlinx.serialization")) {
            "ProGuard rules must preserve Kotlin serialization metadata."
        }
        check(proguardRulesText.contains("androidx.room")) {
            "ProGuard rules must preserve Room metadata or explicitly document why the dependency rules are sufficient."
        }
    }
}

tasks.named("check") {
    dependsOn(verifyAndroidBackupPolicy, verifyAndroidReleaseConfiguration)
}
