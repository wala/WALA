// The root project and its standard subprojects automatically use the version catalog in
// `grable/libs.versions.toml`. To use the same version catalog for the `buildSrc` project, we must
// configure it explicitly.
dependencyResolutionManagement.versionCatalogs
    .create("libs")
    .from(files("../gradle/libs.versions.toml"))
