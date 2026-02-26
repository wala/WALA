import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

val extraTestResources by configurations.registering { isCanBeConsumed = false }

dependencies {
  extraTestResources(project(":cast:js", "testResources"))
  api(libs.rhino)
  api(projects.cast)
  api(projects.cast.js)
  api(projects.core)
  api(projects.util)
  testFixturesApi(libs.junit.jupiter.api)
  testFixturesApi(projects.cast.js)
  testFixturesApi(projects.util)
  testFixturesApi(testFixtures(projects.cast.js))
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(projects.cast)
  testFixturesImplementation(projects.core)
  testImplementation(libs.assertj.core)
  testImplementation(libs.gson)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(testFixtures(projects.cast.js))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") {
  maxHeapSize = "800M"

  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    events("passed", "skipped", "failed")
  }

  if (
      providers.gradleProperty("excludeRequiresInternetTests").isPresent ||
          gradle.startParameter.isOffline ||
          environment["CI"] == "true"
  ) {
    useJUnitPlatform { excludeTags("requires-Internet") }
  }

  outputs.files(layout.buildDirectory.files("actual.dump", "expected.dump"))
}
