import java.io.ByteArrayOutputStream
import org.gradle.kotlin.dsl.support.serviceOf

pluginManagement {
  plugins.id("org.gradle.toolchains.foojay-resolver-convention") version
      settings.rootDir.resolve("foojay-resolver-convention-version.txt").readText().trim()
}

buildscript { dependencies.classpath("com.diffplug.spotless:spotless-lib-extra:4.0.0") }

plugins {
  id("com.diffplug.configuration-cache-for-platform-specific-build") version "4.3.0"
  id("com.gradle.develocity") version "4.2"
  id("org.gradle.toolchains.foojay-resolver-convention")
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
    "ide:jsdt",
    "ide:jsdt:tests",
    "ide:tests",
    "scandroid",
    "shrike",
    "util",
)

develocity.buildScan {
  val isBuildScan = startParameter.isBuildScan
  publishing.onlyIf { isBuildScan }

  if (isBuildScan) {
    val execOps = serviceOf<ExecOperations>()
    background {
      val outputStream = ByteArrayOutputStream()
      outputStream.use {
        execOps.exec {
          commandLine("git", "describe", "--abbrev=0", "--always", "--dirty", "--match=")
          standardOutput = it
        }
      }
      value("Git Commit ID", outputStream.toString().trim())
    }
  }
}
