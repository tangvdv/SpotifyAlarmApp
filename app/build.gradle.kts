plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.spotifyalarm"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.spotifyalarm"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures{
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.preference:preference:1.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.spotify.android:auth:1.2.5")

    implementation("com.google.code.gson:gson:2.6.1")

    implementation("com.android.volley:volley:1.2.1")

    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.airbnb.android:paris:2.0.0")
}