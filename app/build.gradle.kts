import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Gemini API key is no longer baked into the APK — the user pastes their own key
// in Settings → AI Quick Entry, which validates it against Gemini before storing
// it in DataStore. No hardcoded fallback, no local.properties dependency.
@Suppress("unused")
val localProps =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

android {
    namespace = "com.subramanya.artha"
    // compileSdk = 35 to satisfy AndroidX core ≥ 1.15. targetSdk stays at 34
    // per CLAUDE.md (runtime behavior); compileSdk only affects which APIs
    // the compiler can see.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.subramanya.artha"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room exports each schema version as JSON into app/schemas. These are committed so
    // MigrationTestHelper can validate that a migration lands on the expected schema, and
    // so future schema diffs are reviewable. Wired to ksp via room.schemaLocation below.
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Export Room schemas to app/schemas/<dbVersion>.json so MigrationTestHelper can validate
// migrations against the real generated schema. Must be paired with exportSchema = true on
// @Database (see AppDatabase).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room (entities arrive in Session 2; plugin wired now so the catalogue is ready)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (Session 4 onboarding persists userName here)
    implementation(libs.androidx.datastore.preferences)

    // kotlinx
    implementation(libs.kotlinx.datetime)

    // Charts
    implementation(libs.vico.compose.m3)

    // Google Generative AI — backs Phase 3's AI Quick Entry. Key from local.properties.
    implementation(libs.generative.ai)

    // Phase 5 hardening — biometric prompt + encrypted SharedPreferences + Fragment
    // (BiometricPrompt requires a FragmentActivity host).
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.fragment.ktx)

    // Downloadable Google Fonts — Plus Jakarta Sans, Instrument Serif,
    // IBM Plex Mono, Tiro Devanagari Hindi.
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    // Android stubs out org.json in unit tests (every method throws). Pull in the
    // real lib so the RuleSpec JSON codec can round-trip in plain JVM tests.
    testImplementation("org.json:json:20231013")

    // Instrumentation testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // MigrationTestHelper for instrumented Room migration tests.
    androidTestImplementation(libs.androidx.room.testing)

    // Debug-only tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
