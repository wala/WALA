package com.ibm.wala.gradle

// Build configuration for subprojects that include JMH benchmarks.

plugins { id("me.champeau.jmh") }

dependencies.jmhAnnotationProcessor(catalogLibrary("nullaway"))

jmh {
  includeTests = false
  resultFormat = "JSON"

  // If `jmhSmokeCheck` is set, then do the bare minimum needed to verify that the benchmarks
  // execute correctly, i.e., without throwing exceptions.  The performance data that results from
  // such a run is meaningless.  However, a smoke-check run can be prudent before committing many
  // minutes or hours to a full, statistically valid run.
  if (providers.gradleProperty("jmhSmokeCheck").isPresent) {
    fork = 1
    iterations = 1
    operationsPerInvocation = 1
    timeOnIteration = "1ns"
    warmup = "1ns"
    warmupIterations = 1
  }
}
