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

tasks.register("verifyOkHttpDependencyAlignment") {
    val expectedVersion = providers.gradleProperty("raygunOkHttpVersion")
    val resolvedVersions =
        listOf("debugCompileClasspath", "debugRuntimeClasspath").associateWith { name ->
            configurations.named(name).map { configuration ->
                configuration.incoming.resolutionResult.allComponents
                    .mapNotNull { it.moduleVersion }
                    .filter { it.group == "com.squareup.okhttp3" }
                    .associate { it.name to it.version }
            }
        }

    doLast {
        resolvedVersions.forEach { (classpath, resolved) ->
            val versions = resolved.get()
            check(versions.keys.containsAll(setOf("okhttp", "okhttp-bom", "okhttp-urlconnection"))) {
                "$classpath is missing expected OkHttp modules: $versions"
            }
            check(versions.values.all { it == expectedVersion.get() }) {
                "Expected all OkHttp modules on $classpath to use ${expectedVersion.get()}, resolved $versions"
            }
        }
    }
}
