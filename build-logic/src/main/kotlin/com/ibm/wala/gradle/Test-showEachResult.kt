package com.ibm.wala.gradle

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

/**
 * Logs the outcome of every test this [Test] task runs, with the full stack trace of any failure.
 *
 * Opt-in rather than applied by `java.gradle.kts`, because the resulting output is user-visible and
 * only some subprojects want it.
 */
fun Test.showEachResult() = testLogging {
  exceptionFormat = TestExceptionFormat.FULL
  events("passed", "skipped", "failed")
}
