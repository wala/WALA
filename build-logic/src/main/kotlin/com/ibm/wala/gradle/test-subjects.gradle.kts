/**
 * Configures a `testSubjects` source set for Java code that will be analyzed by WALA tests. Sets up
 * both standard Java compilation and ECJ-based compilation for these test subjects.
 */
package com.ibm.wala.gradle

import net.ltgt.gradle.errorprone.errorprone

plugins { id("com.ibm.wala.gradle.java") }

val testSubjects: SourceSet by sourceSets.creating

val compileTestSubjectsJavaUsingEcj = JavaCompileUsingEcj.withSourceSet(project, testSubjects)

tasks {
  named<JavaCompile>("compileTestSubjectsJava") {
    // No need to run Error Prone on our analysis test inputs
    options.errorprone.isEnabled = false
  }

  named("check") { dependsOn(compileTestSubjectsJavaUsingEcj) }
}
