import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.example.travelcents"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.travelcents"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String", "LLM_API_KEY",
            "\"${localProperties.getProperty("LLM_API_KEY") ?: localProperties.getProperty("GROQ_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "LLM_BASE_URL",
            "\"${localProperties.getProperty("LLM_BASE_URL") ?: "https://api.groq.com/openai/v1/"}\""
        )
        buildConfigField(
            "String", "LLM_MODEL",
            "\"${localProperties.getProperty("LLM_MODEL") ?: "llama-3.3-70b-versatile"}\""
        )
        buildConfigField(
            "String", "LLM_INTAKE_MODEL",
            "\"${localProperties.getProperty("LLM_INTAKE_MODEL") ?: "openai/gpt-oss-20b"}\""
        )
        buildConfigField(
            "String", "SERP_API_KEY",
            "\"${localProperties.getProperty("SERP_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "YELP_API_KEY",
            "\"${localProperties.getProperty("YELP_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "MAPBOX_TOKEN",
            "\"${localProperties.getProperty("MAPBOX_TOKEN") ?: ""}\""
        )
        buildConfigField(
            "String", "BESTTIME_API_KEY",
            "\"${localProperties.getProperty("BESTTIME_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "GOOGLE_DIRECTIONS_KEY",
            "\"${localProperties.getProperty("GOOGLE_DIRECTIONS_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "WALKSCORE_API_KEY",
            "\"${localProperties.getProperty("WALKSCORE_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "TICKETMASTER_API_KEY",
            "\"${localProperties.getProperty("TICKETMASTER_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "UNSPLASH_ACCESS_KEY",
            "\"${localProperties.getProperty("UNSPLASH_ACCESS_KEY") ?: ""}\""
        )
        buildConfigField(
            "String", "OPENSKY_CLIENT_ID",
            "\"${localProperties.getProperty("OPENSKY_CLIENT_ID") ?: ""}\""
        )
        buildConfigField(
            "String", "OPENSKY_CLIENT_SECRET",
            "\"${localProperties.getProperty("OPENSKY_CLIENT_SECRET") ?: ""}\""
        )
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.firebase.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.androidx.navigation.compose)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Coroutines support for Firebase Tasks (.await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Retrofit + OkHttp for AI provider integrations
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Ktor Networking
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-client-android:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.play.services.location)
    implementation("sh.calvin.reorderable:reorderable:2.4.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}
