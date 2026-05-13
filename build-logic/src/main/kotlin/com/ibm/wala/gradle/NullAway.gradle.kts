package com.ibm.wala.gradle

import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("com.ibm.wala.gradle.java")
  id("java-library")
  id("net.ltgt.errorprone")
}

dependencies {
  annotationProcessor(catalogLibrary("nullaway"))
  compileOnly(catalogLibrary("nullaway-annotations"))
}

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    if (!name.contains("test", true)) {
      error("NullAway")
      error("RequireExplicitNullMarking")
      errorproneArgs.addAll(
          "-XepOpt:NullAway:OnlyNullMarked=true",
          "-XepOpt:NullAway:JSpecifyMode=true",
          "-XepOpt:NullAway:CastToNonNullMethod=com.ibm.wala.util.nullability.NullabilityUtil.castToNonNull",
      )
    }
  }
}
