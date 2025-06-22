plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.example.smartmoodjournal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartmoodjournal"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true // ✅ Enables BuildConfig injection
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.8"
    }

    // ✅ Inject Gemini API Key from local.properties
    val geminiApiKey: String = rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.find { it.startsWith("GEMINI_API_KEY=") }
        ?.substringAfter("=")
        ?.trim()
        ?: System.getenv("GEMINI_API_KEY") ?: ""

    buildTypes.forEach {
        it.buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
// Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Lifecycle + Navigation
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Firebase + Auth + Firestore
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Image Loading
    implementation("com.squareup.picasso:picasso:2.8")

    // Networking + Coroutines
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
