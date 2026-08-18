package com.ibm.wala.gradle

/**
 * Build configuration for subprojects that include JMH benchmarks.
 *
 * Applies JMH and WALA Java plugins, then configures benchmark result output to a JSON file in the
 * build directory. Optionally appends the current Git commit hash to the result file name when the
 * project property `jmhResultsFileNameIncludesGitHash` is enabled. Disables compiler
 * warnings-as-errors and Error Prone for JMH-generated classes to accommodate auto-generated
 * benchmark code.
 */
import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("me.champeau.jmh")
  id("com.ibm.wala.gradle.java")
}

/**
 * Indicates whether the Git commit hash should be included in the benchmark results file name.
 *
 * Evaluates the `jmhResultsFileNameIncludesGitHash` Gradle property, defaulting to `false` if not
 * set.
 */
private val includeGitHash =
    providers
        .gradleProperty("jmhResultsFileNameIncludesGitHash")
        .map { it.isEmpty() || it.toBooleanStrictOrNull() != false }
        .orElse(false)

/**
 * Suffix to append to the benchmark results file name.
 *
 * When [includeGitHash] is enabled, this is `"-"` followed by the Git commit description;
 * otherwise, it is an empty string.
 */
private val resultsFileSuffix =
    includeGitHash.flatMap { enabled ->
      if (enabled) {
        providers
            .exec {
              commandLine("git", "describe", "--abbrev=0", "--always", "--dirty", "--match=")
            }
            .standardOutput
            .asText
            .map { "-${it.trim()}" }
      } else {
        providers.provider { "" }
      }
    }

jmh {
  jmhVersion = catalogVersion("jmh")
  resultFormat = "JSON"
  resultsFile =
      resultsFileSuffix.flatMap { layout.buildDirectory.file("results/jmh/results$it.json") }
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
