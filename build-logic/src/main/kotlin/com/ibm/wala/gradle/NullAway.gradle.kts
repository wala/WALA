package com.ibm.wala.gradle

import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("com.ibm.wala.gradle.java")
  id("java-library")
  id("net.ltgt.errorprone")
}

dependencies {
  annotationProcessor(versionCatalogs.named("libs").findLibrary("nullaway").get())
  compileOnly(versionCatalogs.named("libs").findLibrary("nullaway-annotations").get())
}

tasks.withType<JavaCompile> {
  options.errorprone {
    if (!name.contains("test", true)) {
      error("NullAway")
      errorproneArgs.addAll(
          "-XepOpt:NullAway:AnnotatedPackages=com.ibm.wala",
          "-XepOpt:NullAway:JSpecifyMode=true",
          "-XepOpt:NullAway:CastToNonNullMethod=com.ibm.wala.util.nullability.NullabilityUtil.castToNonNull",
      )
    }
  }
}
