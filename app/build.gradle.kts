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
}

dependencies {
    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")

    // --- ZXing ---
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // --- Image Loading ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- Navigation & UI ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // --- Unit tests (JVM) ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.4.2")

    // --- Instrumented Android UI tests ---
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.6.1")

    // --- Orchestrator (optional for stable test runs) ---
    androidTestUtil("androidx.test:orchestrator:1.5.1")
}