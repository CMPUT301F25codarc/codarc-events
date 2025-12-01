plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "ca.ualberta.codarc.codarc_events"
    compileSdk = 36


    defaultConfig {
        applicationId = "ca.ualberta.codarc.codarc_events"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    // --- Firebase (using BOM for version management) ---
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    
    // --- QR Code Scanning ---
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // --- Image Loading ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- HTTP & JSON ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // --- Navigation & UI ---
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core:1.13.1")

    // --- Google Maps & Location ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- Unit tests (runs on JVM) ---
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.4.2")

    // --- Instrumented Android UI tests ---
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    /*
       Author: Vladimir Fisher https://stackoverflow.com/users/4975634/vladimir-fisher
       Title: java.lang.NoSuchMethodError: No static method registerDefaultInstance with Firebase Performance and Espresso Instrumented Tests
       Answer: https://stackoverflow.com/a/68675881
       Date: 2021-02-11
       License: CC-BY-SA 4.0 (International)
    */
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.6.1")

    // --- Optional for stable instrumentation ---
    androidTestUtil("androidx.test:orchestrator:1.5.1")
}