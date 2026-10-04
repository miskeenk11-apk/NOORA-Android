plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace="com.noora.assistant"
    compileSdk=35

    defaultConfig {
        applicationId="com.noora.assistant"
        minSdk=26
        targetSdk=35
        versionCode=1
        versionName="0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("io.github.sceneview:sceneview:4.52.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.msnilsen:openwakeword-android:0.1.0")
}
