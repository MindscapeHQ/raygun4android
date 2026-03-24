# AGP 9.0.1 Migration Plan

**Issue:** [#256](https://github.com/MindscapeHQ/raygun4android/issues/256)
**Branch:** `256-migrate-agp-9`
**From:** AGP 8.13.2 → AGP 9.0.1
**Ref:** Dependabot PR [#254](https://github.com/MindscapeHQ/raygun4android/pull/254) (version-only bump — insufficient)

---

## Approach

All changes are applied **atomically**. The build is not expected to work between individual steps — AGP 9.0 requires simultaneous removal of `kotlin-android`, legacy variant APIs, and deprecated DSL.

We are doing a **full migration** — no opt-out flags (`android.newDsl=false`, `android.builtInKotlin=false`). These would only defer work to AGP 10.0 (mid-2026).

---

## Steps

### Step 1 — Version catalog (`gradle/libs.versions.toml`)

- [ ] Bump `agp` from `"8.13.2"` to `"9.0.1"`
- [ ] Remove `kotlin-android` plugin alias from `[plugins]` section
- [ ] Keep `kotlin` version entry (`"2.3.0"`) — used to pin KGP via buildscript classpath
- [ ] Keep `kotlin-gradle` library entry — used by root `build.gradle` classpath

**Why keep `kotlin` version?** AGP 9 bundles KGP 2.2.10, but we want 2.3.0. The `classpath(libs.kotlin.gradle)` in root `build.gradle` overrides the bundled version.

### Step 2 — Root build script (`build.gradle`)

- [ ] Remove `alias(libs.plugins.kotlin.android) apply false` from `plugins {}`
- [ ] Keep `classpath(libs.kotlin.gradle)` in `buildscript.dependencies` (KGP pinning)

### Step 3 — Provider module (`provider/build.gradle`)

- [ ] **3a.** Remove `apply plugin: 'kotlin-android'`
- [ ] **3b.** Rename `minSdkVersion` → `minSdk`, `targetSdkVersion` → `targetSdk`
- [ ] **3c.** Remove `kotlinOptions { jvmTarget = "17" }` block
- [ ] **3c.** Remove `kotlin { jvmToolchain(17) }` block
- [ ] **3d.** Replace `afterEvaluate { android.libraryVariants... }` AAR renaming with `androidComponents { onVariants(...) }` API
- [ ] **3e.** Remove entire `androidJavadocs` task block (lines 56–68)

**3d detail — AAR renaming replacement:**
```groovy
// OLD (legacy variant API — removed in AGP 9)
afterEvaluate {
    android.libraryVariants.all { variant ->
        def variantName = variant.name
        def capitalizedName = variantName.capitalize()
        tasks.named("bundle${capitalizedName}Aar").configure {
            archiveFileName.set(variantName == 'debug' ? "raygun4android-debug.aar" : "raygun4android.aar")
        }
    }
}

// NEW (androidComponents API — stable since AGP 7.x)
androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        tasks.named("bundle${variant.name.capitalize()}Aar").configure {
            archiveFileName.set("raygun4android-debug.aar")
        }
    }
    onVariants(selector().withBuildType("release")) { variant ->
        tasks.named("bundle${variant.name.capitalize()}Aar").configure {
            archiveFileName.set("raygun4android.aar")
        }
    }
}
```

**3e rationale — removing `androidJavadocs`:**
- Uses `android.libraryVariants` and `variant.javaCompileProvider` — both removed in AGP 9's new DSL
- Task targets `java.srcDirs` but the module is Kotlin-first (all source is `.kt`)
- Javadoc produces nothing useful for Kotlin; Dokka is the proper tool
- Task is not wired into the publishing pipeline
- **Follow-up:** Consider adding Dokka v2 if published API docs are needed

### Step 4 — App module (`app/build.gradle`)

- [ ] **4a.** Remove `apply plugin: 'kotlin-android'`
- [ ] **4b.** Rename `minSdkVersion` → `minSdk`, `targetSdkVersion` → `targetSdk`
- [ ] **4c.** Replace `lintOptions { disable ... }` with `lint { disable += [...] }`
- [ ] **4d.** Remove `kotlinOptions { jvmTarget = "17" }` block
- [ ] **4d.** Remove `kotlin { jvmToolchain(17) }` block

**4c detail — lint migration:**
```groovy
// OLD
lintOptions {
    disable 'LogNotTimber','StringFormatInTimber', ...
}

// NEW
lint {
    disable += ['LogNotTimber', 'StringFormatInTimber', ...]
}
```

### Step 5 — Gradle properties (`gradle.properties`)

- [ ] Remove `android.nonFinalResIds=false`

**Why safe to remove:** No Java sources with `switch` over app `R` constants. The project uses Kotlin `when` expressions which don't require compile-time constants.

**Not adding:**
- `android.newDsl=false` — not needed, all legacy API usage is being migrated
- `android.builtInKotlin=false` — not needed, `kotlin-android` plugin is being removed

### Step 6 — Documentation (`README.md`)

- [ ] Update `lintOptions` example to `lint` syntax

### Files reviewed — no changes needed

- [x] `settings.gradle` — OK
- [x] `gradle/wrapper/gradle-wrapper.properties` — Already Gradle 9.3.1 ✅
- [x] `provider/maven-central-publish.gradle` — No variant API usage ✅
- [x] `provider/gradle.properties` — OK
- [x] `spotless.gradle` — OK
- [x] All `AndroidManifest.xml` files — OK
- [x] Both `proguard-rules.pro` files — OK

---

## Validation Checklist

Run after all steps are complete:

- [ ] `./gradlew help` — Configuration/sync succeeds
- [ ] `./gradlew :provider:bundleDebugAar` — Produces `raygun4android-debug.aar`
- [ ] `./gradlew :provider:bundleReleaseAar` — Produces `raygun4android.aar`
- [ ] `./gradlew :app:assembleDebug` — App debug build succeeds
- [ ] `./gradlew :app:assembleRelease` — App release build succeeds
- [ ] `./gradlew test` — Unit tests pass
- [ ] `./gradlew lint` — Lint passes
- [ ] `./gradlew :provider:publishToMavenLocal` — Publishing works
- [ ] `./gradlew :app:tasks --all` — `notifyDeployment` and `uploadProguardMapping` tasks exist

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| AAR rename task names changed | Build fails at rename step | Verify `bundleDebugAar`/`bundleReleaseAar` exist post-migration |
| Library consumers need `compileSdk >= 36` | Consumer build failures | Test with `publishToMavenLocal` + consumer project; add explicit `aarMetadata.minCompileSdk` if needed |
| `nonFinalResIds` removal | Compilation errors if `R` constants used in `switch` | Compilation will surface immediately; restore property temporarily if needed |

---

## Follow-up (out of scope)

- [ ] Consider adding Dokka v2 for Kotlin API documentation
- [ ] Close Dependabot PR #254 after this branch is merged
- [ ] Evaluate `aarMetadata.minCompileSdk` for consumer compatibility
