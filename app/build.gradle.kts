plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vknxworks.seerrctrl"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vknxworks.seerrctrl"
        minSdk = 35
        targetSdk = 36
        // CI passes the run number so every build installs over the previous one.
        // Bump both to publish a new release (see CHANGELOG.md).
        versionCode = 57
        versionName = "1.12.2"
        // 64-bit ARM phones only (arm64-v8a), like every Ctrl app.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Release signing. On GitHub Actions the build job has no signing key at all: it sets
    // UNSIGNED_RELEASE=true and gets app-release-unsigned.apk, which a separate job signs with apksigner
    // (see .github/workflows/build.yml). An unsigned APK cannot be installed, so nothing unsigned can ship.
    // A keystore from environment variables still works (SIGNING_KEYSTORE_PATH, SIGNING_STORE_PASSWORD,
    // SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD). Local builds without one fall back to the debug key; on CI
    // a build that has neither a key nor UNSIGNED_RELEASE=true fails, so a debug-signed APK is never
    // published as a release.
    val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
    val onCi = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
    val unsignedRelease = System.getenv("UNSIGNED_RELEASE") == "true"
    signingConfigs {
        if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8: strips unused code (most of the icon set) and resources, and optimises the rest.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val release = signingConfigs.findByName("release")
            if (release == null && onCi && !unsignedRelease) {
                throw GradleException(
                    "Release signing values are missing on CI and UNSIGNED_RELEASE=true is not set; " +
                        "refusing to sign the release with the debug key",
                )
            }
            // UNSIGNED_RELEASE=true: no signing here at all, the output is app-release-unsigned.apk.
            signingConfig = if (unsignedRelease) null else (release ?: signingConfigs.getByName("debug"))
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

    // F-Droid: no dependency-info block in the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.window:window:1.3.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    val coil = "3.1.0"
    implementation("io.coil-kt.coil3:coil-compose:$coil")
    implementation("io.coil-kt.coil3:coil-network-okhttp:$coil")
}
