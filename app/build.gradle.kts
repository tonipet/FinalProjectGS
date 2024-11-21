plugins {

    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gamemonitoringapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gamemonitoringapp"
        minSdk = 23
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        // Optionally exclude other problematic files
        // exclude 'META-INF/*.kotlin_module'
        // exclude 'META-INF/NOTICE'
        // exclude 'META-INF/LICENSE'
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.7.1") //for workier
    //implementation ("com.android.volley:volley:1.2.1") //Dependency for API

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.recyclerview)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.0.3")
    implementation ("com.google.android.material:material:1.7.0")
    //implementation("com.vonage:client:6.1.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
}