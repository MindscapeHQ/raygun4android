dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri(providers.gradleProperty("raygunRepository").get())
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "okhttp-alignment-test"
