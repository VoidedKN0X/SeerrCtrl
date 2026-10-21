plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vknxworks.seerrctrl"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vknxworks.seerrctrl"
        minSdk = 33
        targetSdk = 35
        // CI passes the run number so every build installs over the previous one.
        // Bump both to publish a new release (see CHANGELOG.md).
        versionCode = 29
        versionName = "1.5.2"
        // 64-bit ARM phones only (arm64-v8a), like every Ctrl app.
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    // Release signing from env (GitHub secrets). Local builds fall back to the debug key; CI never does,
    // so a published release is always signed with the real key.
    val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
    val onCi = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
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
            if (release == null && onCi) {
                throw GradleException("Release signing values are missing on CI; refusing to sign the release with the debug key")
            }
            signingConfig = release ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.window:window:1.3.0")
    // Backdrop blur behind the floating tab bar.
    implementation("dev.chrisbanes.haze:haze:1.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    val coil = "3.1.0"
    implementation("io.coil-kt.coil3:coil-compose:$coil")
    implementation("io.coil-kt.coil3:coil-network-okhttp:$coil")
}
