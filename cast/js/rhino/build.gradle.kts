import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

val extraTestResources: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  extraTestResources(project(mapOf("path" to ":cast:js", "configuration" to "testResources")))
  implementation(libs.rhino)
  implementation(projects.cast)
  implementation(projects.cast.js)
  implementation(projects.core)
  implementation(projects.util)
  testImplementation(libs.gson)
  testImplementation(libs.hamcrest)
  testImplementation(libs.junit)
  testImplementation(testFixtures(projects.cast))
  testImplementation(testFixtures(projects.cast.js))
  testFixturesImplementation(libs.junit)
  testFixturesImplementation(testFixtures(projects.cast))
  testFixturesImplementation(testFixtures(projects.cast.js))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") {
  maxHeapSize = "800M"

  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    events("passed", "skipped", "failed")
  }

  if (project.hasProperty("excludeRequiresInternetTests") ||
      gradle.startParameter.isOffline ||
      environment.get("CI") == "true") {
    useJUnit { excludeCategories("com.ibm.wala.cast.js.test.RequiresInternetTests") }
  }

  outputs.files(layout.buildDirectory.files("actual.dump", "expected.dump"))
}
