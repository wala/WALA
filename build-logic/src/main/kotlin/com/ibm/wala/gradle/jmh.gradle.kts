package com.ibm.wala.gradle

// Build configuration for subprojects that include JMH benchmarks.

import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("me.champeau.jmh")
  id("com.ibm.wala.gradle.java")
}

jmh {
  jmhVersion = catalogVersion("jmh")
  resultFormat = "JSON"
}

tasks {
  // JMH-generated code is exempt from the strict linter scruitiny applied to human-authored code.
  named<JavaCompile>("jmhCompileGeneratedClasses") {
    options.run {
      compilerArgs.remove("-Werror")
      errorprone.enabled = false
    }
  }
}
