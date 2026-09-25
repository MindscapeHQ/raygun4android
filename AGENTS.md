# Agent guidance

## Published SDK compatibility

Before updating Kotlin, AGP, the Maven publishing plugin, or `:provider` dependencies, read **Published Kotlin compatibility** in `CONTRIBUTING.md`.

- Keep the build Kotlin Gradle plugin version separate from the Kotlin versions published to SDK consumers.
- Do not raise the provider's `coreLibrariesVersion`, `apiVersion`, or `languageVersion` during routine dependency updates.
- Refresh dependency locks and verification metadata after dependency changes.
- Preserve and run the Kotlin 2.1 published-consumer integration test. Do not bypass metadata checks or force consumer dependency versions to make it pass.
