import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
}

// Used for Raygun notifyDeployment task. In the real world this would come from a local user.properties file etc.
val localUserName = "Change Me"
val localUserEmail = "changeme@test.com"

// Raygun Gradle task configuration
val RAYGUN_GROUP = "raygun"
val RAYGUN_API_TOKEN = "<YOUR EXTERNAL ACCESS TOKEN>"
val RAYGUN_API_KEY = "<YOUR RG APP API KEY>"
val RAYGUN_APP_PATH = "<YOUR RG APP UPLOAD PATH>"

val VERSION_CODE: String by project
val VERSION_NAME: String by project
val sampleVersionName = "$VERSION_NAME-sample"

/*
 * Setup a sample Gradle task for deployment notification
 * This is done as a function so that there's more flexibility when it comes to dealing with creating the tasks for multiple flavours etc.
 */
fun Project.registerRaygunNotifyDeploymentTask(
    token: String,
    key: String,
    groupName: String,
    version: String,
    userName: String,
    userEmail: String,
) {
    tasks.register("notifyDeployment") {
        group = groupName

        doLast {
            val url = URI("https://app.raygun.io/deployments?authToken=$token").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body =
                """{"apiKey":"$key","version":"$version","ownerName":"$userName","emailAddress":"$userEmail"}"""
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

            val response =
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            println(response)
        }
    }
}

/*
 * Setup a sample Gradle task for upload proguard mapping files
 * This is done as a function so that there's more flexibility when it comes to dealing with creating the tasks for multiple flavours etc.
 */
fun Project.registerRaygunProguardTask(
    token: String,
    raygunAppPath: String,
    groupName: String,
    version: String,
) {
    tasks.register("uploadProguardMapping") {
        group = groupName

        doLast {
            val mappingFile =
                file("${project.rootDir}/app/build/outputs/mapping/release/mapping.txt")
            check(mappingFile.exists()) { "Mapping file not found: ${mappingFile.absolutePath}" }

            val boundary = "----RaygunBoundary${System.currentTimeMillis()}"
            val endpointUrl =
                "https://app.raygun.com/upload/proguardsymbols/$raygunAppPath?authToken=$token"
            val url = URI(endpointUrl).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary",
            )
            connection.doOutput = true

            connection.outputStream.use { out ->
                val writer = PrintWriter(OutputStreamWriter(out, Charsets.UTF_8), true)

                // version field
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"version\"\r\n\r\n")
                writer.append("$version\r\n")

                // file field
                writer.append("--$boundary\r\n")
                writer.append(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"${mappingFile.name}\"\r\n",
                )
                writer.append("Content-Type: application/octet-stream\r\n\r\n")
                writer.flush()
                mappingFile.inputStream().use { input -> input.copyTo(out) }
                out.flush()
                writer.append("\r\n")

                writer.append("--$boundary--\r\n")
                writer.flush()
            }

            val responseCode = connection.responseCode
            val result =
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            println("HTTP $responseCode: $result")
            check(result == "true") { "Unexpected Raygun response: $result" }
        }
    }
}

val keystoreFile = file("testkeystore.jks")

android {
    namespace = "com.raygun.raygun4android.sample"
    compileSdk = 36

    val releaseSigning =
        if (keystoreFile.exists()) {
            signingConfigs.create("releaseConfig") {
                keyAlias = "testkey"
                keyPassword = "123456"
                storeFile = keystoreFile
                storePassword = "123456"
            }
        } else {
            null
        }

    defaultConfig {
        applicationId = "com.raygun.raygun4android"
        minSdk = 23
        targetSdk = 36
        versionCode = VERSION_CODE.toInt()
        versionName = sampleVersionName
        buildConfigField("long", "VERSION_CODE", VERSION_CODE)
        buildConfigField("String", "VERSION_NAME", "\"$sampleVersionName\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        disable +=
            setOf(
                "LogNotTimber",
                "StringFormatInTimber",
                "ThrowableNotAtBeginning",
                "BinaryOperationInTimber",
                "TimberArgCount",
                "TimberArgTypes",
                "TimberTagLength",
            )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            releaseSigning?.let { signingConfig = it }
        }
        getByName("debug") {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

registerRaygunNotifyDeploymentTask(
    RAYGUN_API_TOKEN,
    RAYGUN_API_KEY,
    RAYGUN_GROUP,
    sampleVersionName,
    localUserName,
    localUserEmail,
)

registerRaygunProguardTask(
    RAYGUN_API_TOKEN,
    RAYGUN_APP_PATH,
    RAYGUN_GROUP,
    sampleVersionName,
)

dependencies {
    // Usage option A: Use and build from local provider library src
    implementation(project(":provider"))
    // Usage option B: Use and build from external repo
    // 1. Use
    // implementation("com.raygun:raygun4android:x.y.z")
    // or
    // 2. Change version in Gradle library register and use
    // implementation(libs.raygun)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestDebugImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)

    debugImplementation(libs.leakcanary)
}
