/**
 * Configures a `testSubjects` source set for Java code that will be analyzed by WALA tests.
 * Standard and ECJ-based compilation for the new source set are both registered automatically by
 * the `com.ibm.wala.gradle.java` plugin; this plugin only disables Error Prone for the test
 * subjects.
 */
package com.ibm.wala.gradle

import net.ltgt.gradle.errorprone.errorprone

plugins { id("com.ibm.wala.gradle.java") }

val testSubjects = sourceSets.create("testSubjects")

tasks {
  named<JavaCompile>("compileTestSubjectsJava") {
    // No need to run Error Prone on our analysis test inputs
    options.errorprone.enabled = false
  }
}
