package com.ibm.wala.gradle

import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("com.ibm.wala.gradle.java")
  id("java-library")
  id("net.ltgt.errorprone")
}

dependencies { annotationProcessor("com.uber.nullaway:nullaway:0.10.5") }

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    if (!name.toLowerCase().contains("test")) {
      error("NullAway")
      errorproneArgs.addAll(
          "-XepOpt:NullAway:AnnotatedPackages=com.ibm.wala",
      )
    }
  }
}
