buildscript { dependencies { classpath("com.diffplug.spotless:spotless-lib-extra:2.43.1") } }

plugins {
  id("com.diffplug.configuration-cache-for-platform-specific-build") version "3.44.0"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "wala"

includeBuild("build-logic")

include(
    "cast",
    "cast:smoke_main",
    "cast:xlator_test",
    "cast:cast",
    "cast:java",
    "cast:java:ecj",
    "cast:java:test:data",
    "cast:js",
    "cast:js:html:nu_validator",
    "cast:js:nodejs",
    "cast:js:rhino",
    "code-coverage-report",
    "core",
    "dalvik",
    "ide",
    "ide:jdt",
    "ide:jdt:test",
    "ide:jsdt",
    "ide:jsdt:tests",
    "ide:tests",
    "scandroid",
    "shrike",
    "util",
)
