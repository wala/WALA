package com.ibm.wala.gradle

import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("com.ibm.wala.gradle.java")
  id("java-library")
  id("net.ltgt.errorprone")
}

dependencies {
  annotationProcessor(rootProject.versionCatalogs.named("libs").findLibrary("nullaway").get())
}

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    if (!name.contains("test", true)) {
      error("NullAway")
      errorproneArgs.addAll(
          "-XepOpt:NullAway:AnnotatedPackages=com.ibm.wala",
      )
    }
  }
}
