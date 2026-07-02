package com.ibm.wala.gradle

// Build configuration shared by all projects *including* the root project.

plugins {
  eclipse
  id("com.diffplug.spotless")
}

repositories.mavenCentral()

////////////////////////////////////////////////////////////////////////
//
//  Helpers for dependency locking
//

// this task resolves dependencies in all sub-projects, making it easy to
// generate lockfiles
tasks.register<DependencyReportTask>("allDeps") {
  description = "Resolve all project dependencies for lockfile generation"
}

////////////////////////////////////////////////////////////////////////
//
//  Code formatting
//

spotless {
  providers.gradleProperty("spotless.ratchet.from").orNull?.let(::ratchetFrom)

  kotlinGradle { ktfmt(catalogVersion("ktfmt")) }
}
