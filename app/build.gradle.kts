plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fitness.fitnessapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fitness.fitnessapplication"
        minSdk = 28
        targetSdk = 35
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // RxJava Core
    implementation(libs.rxjava) // Check for the latest version

    // RxAndroid (for Android-specific threading)
    implementation(libs.rxandroid)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)


    // Material Components (Material Design library)
    //
    implementation(libs.adapter.rxjava3)
    //zxing
    implementation(libs.zxing.android.embedded)
    implementation(libs.picasso)



}