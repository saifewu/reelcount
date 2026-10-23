plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The first CI build creates this key and commits it, so every later APK
// installs over the previous one and your counts survive updates.
val keystore = rootProject.file("signing/reelcount.p12")
val build = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "app.reelcount"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.reelcount"
        minSdk = 26
        targetSdk = 34
        versionCode = build
        versionName = "1.0.$build"
    }

    signingConfigs {
        create("release") {
            if (keystore.exists()) {
                storeFile = keystore
                storeType = "pkcs12"
                storePassword = "reelcount"
                keyAlias = "reelcount"
                keyPassword = "reelcount"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (keystore.exists()) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        checkReleaseBuilds = false
    }
}
