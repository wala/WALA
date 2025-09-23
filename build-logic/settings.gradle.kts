pluginManagement {
  plugins.id("org.gradle.toolchains.foojay-resolver-convention") version
      settings.rootDir.parentFile
          .resolve("foojay-resolver-convention-version.txt")
          .readText()
          .trim()
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") }

rootProject.name = "build-logic"

// The WALA root project and its standard subprojects automatically use the version catalog in
// `gradle/libs.versions.toml`. To use the same version catalog for this `build-logic` project, we
// must configure it explicitly.
dependencyResolutionManagement.versionCatalogs
    .create("libs")
    .from(files("../gradle/libs.versions.toml"))
