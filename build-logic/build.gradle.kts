@file:Suppress("UnstableApiUsage")

import com.diffplug.spotless.LineEnding.PLATFORM_NATIVE

plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
  alias(libs.plugins.spotless)
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.gradle.download.task)
  implementation(libs.gradle.errorprone.plugin)
  implementation(libs.gradle.goomph.plugin)
  implementation(libs.gradle.spotless.plugin)
}

kotlin.jvmToolchain { languageVersion = JavaLanguageVersion.of(11) }

spotless {
  // Workaround for <https://github.com/diffplug/spotless/issues/1644>
  // using idea found at
  // <https://github.com/diffplug/spotless/issues/1527#issuecomment-1409142798>.
  lineEndings = PLATFORM_NATIVE

  val ktfmtVersion = libs.versions.ktfmt.get()

  kotlin {
    ktfmt(ktfmtVersion)
    targetExclude("build/")
  }

  kotlinGradle { ktfmt(ktfmtVersion) }

  findProperty("spotless.ratchet.from")?.let { ratchetFrom(it as String) }
}
