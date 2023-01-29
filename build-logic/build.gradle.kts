plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.gradle.download.task)
  implementation(libs.gradle.errorprone.plugin)
  implementation(libs.gradle.spotless.plugin)
}

kotlin.jvmToolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
