plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version
      File("foojay-resolver-convention-version.txt").readText().trim()
}

rootProject.name = "build-logic"

// The WALA root project and its standard subprojects automatically use the version catalog in
// `gradle/libs.versions.toml`. To use the same version catalog for this `build-logic` project, we
// must configure it explicitly.
dependencyResolutionManagement.versionCatalogs
    .create("libs")
    .from(files("../gradle/libs.versions.toml"))
