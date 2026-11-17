plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sennara.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sennara.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)        // Material 3 components (cards, chips, buttons, bottom navigation…)
    implementation(libs.recyclerview)    // horizontal carousels and the fish grid
    implementation(libs.work)            // WorkManager: background "great fishing" alerts
    implementation(libs.osmdroid)        // OpenStreetMap map view (no API key needed)
}
