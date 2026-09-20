import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage

plugins { base }

val consumerRuntime =
    configurations.create("consumerRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

dependencies {
    consumerRuntime("com.raygun:raygun4android:${providers.gradleProperty("raygunVersion").get()}")
    consumerRuntime("com.squareup.okhttp3:okhttp-urlconnection:4.9.2")
}

tasks.register("verifyOkHttpAlignment") {
    val resolutionResult = consumerRuntime.incoming.resolutionResult

    doLast {
        val okHttpVersions =
            resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .filter { it.group == "com.squareup.okhttp3" }
                .associate { it.name to it.version }

        val coreVersion = checkNotNull(okHttpVersions["okhttp"]) { "okhttp was not resolved" }
        check(okHttpVersions["okhttp-urlconnection"] == coreVersion) {
            "Expected okhttp-urlconnection $coreVersion, resolved ${okHttpVersions["okhttp-urlconnection"]}"
        }
        check(okHttpVersions["okhttp-java-net-cookiejar"] == coreVersion) {
            "Expected okhttp-java-net-cookiejar $coreVersion, resolved ${okHttpVersions["okhttp-java-net-cookiejar"]}"
        }
    }
}
