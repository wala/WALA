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
  implementation(libs.gradle.errorprone.plugin)
  implementation(libs.gradle.goomph.plugin)
  implementation(libs.gradle.maven.publish.plugin)
  implementation(libs.gradle.spotless.plugin)
}

kotlin.jvmToolchain { languageVersion = JavaLanguageVersion.of(11) }

spotless {
  val ktfmtVersion = libs.versions.ktfmt.get()

  kotlin {
    ktfmt(ktfmtVersion)
    targetExclude("build/")
  }

  kotlinGradle { ktfmt(ktfmtVersion) }

  findProperty("spotless.ratchet.from")?.let { ratchetFrom(it as String) }
}
