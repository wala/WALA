plugins {
  `java-library`
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation("de.undercouch:gradle-download-task:5.3.0")
  implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.0.1")
}

kotlin.jvmToolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
