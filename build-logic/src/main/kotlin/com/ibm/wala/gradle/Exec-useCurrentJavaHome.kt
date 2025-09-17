package com.ibm.wala.gradle

import org.gradle.api.tasks.Exec

/** Configures this [Exec] task to run with `JAVA_HOME` set to the current Gradle JVM's home. */
fun Exec.useCurrentJavaHome() {
  environment("JAVA_HOME", project.providers.systemProperty("java.home").valueToString)
}
