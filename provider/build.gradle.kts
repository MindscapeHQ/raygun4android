import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.central.publish)
}

val VERSION_CODE: String by project
val VERSION_NAME: String by project
val GROUP: String by project
val POM_NAME: String by project
val POM_ARTIFACT_ID: String by project
val POM_DESCRIPTION: String by project
val POM_URL: String by project
val POM_SCM_URL: String by project
val POM_SCM_CONNECTION: String by project
val POM_SCM_DEV_CONNECTION: String by project
val POM_LICENCE_NAME: String by project
val POM_LICENCE_URL: String by project
val POM_LICENCE_DIST: String by project
val POM_DEVELOPER_1_ID: String by project
val POM_DEVELOPER_1_NAME: String by project
val POM_DEVELOPER_2_ID: String by project
val POM_DEVELOPER_2_NAME: String by project

android {
    compileSdk = 36
    namespace = "com.raygun.raygun4android"

    defaultConfig {
        minSdk = 23
        buildConfigField("long", "VERSION_CODE", VERSION_CODE)
        buildConfigField("String", "VERSION_NAME", "\"$VERSION_NAME\"")
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<Zip>().configureEach {
    when (name) {
        "bundleDebugAar" -> archiveFileName.set("raygun4android-debug.aar")
        "bundleReleaseAar" -> archiveFileName.set("raygun4android.aar")
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.timber)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}

// Maven Central publishing configuration
// Plugin docs: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
mavenPublishing {
    coordinates(GROUP, POM_ARTIFACT_ID, VERSION_NAME)

    pom {
        name.set(POM_NAME)
        description.set(POM_DESCRIPTION)
        url.set(POM_URL)

        scm {
            url.set(POM_SCM_URL)
            connection.set(POM_SCM_CONNECTION)
            developerConnection.set(POM_SCM_DEV_CONNECTION)
        }

        licenses {
            license {
                name.set(POM_LICENCE_NAME)
                url.set(POM_LICENCE_URL)
                distribution.set(POM_LICENCE_DIST)
            }
        }

        developers {
            developer {
                id.set(POM_DEVELOPER_1_ID)
                name.set(POM_DEVELOPER_1_NAME)
            }
            developer {
                id.set(POM_DEVELOPER_2_ID)
                name.set(POM_DEVELOPER_2_NAME)
            }
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
