plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.raygun.raygun4android.compatibility"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(
        "com.raygun:raygun4android:${providers.gradleProperty("raygunVersion").get()}",
    )
    // The published Raygun BOM must align this deliberately old consumer dependency.
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.9.2")
}
