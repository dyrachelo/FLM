plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mfl"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mfl"
        minSdk = 24
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
    buildFeatures{
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4") // Проверьте на актуальную версию
    implementation(libs.firebase.auth)
    //compose
    implementation ("androidx.compose.material3:material3:1.1.1")
    implementation ("androidx.compose.ui:ui:1.3.0")
    implementation ("androidx.compose.material:material:1.3.0")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.3.0")
    implementation ("androidx.activity:activity-compose:1.6.0")

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-auth-ktx:21.0.1")
    implementation("com.google.android.gms:play-services-auth:20.2.0")

    implementation ("androidx.appcompat:appcompat:1.6.0")
    implementation ("androidx.core:core-ktx:1.10.0")
    implementation ("com.google.firebase:firebase-database-ktx:20.3.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}