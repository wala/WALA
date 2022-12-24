import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

val extraTestResources: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  extraTestResources(
      project(mapOf("path" to ":com.ibm.wala.cast.js", "configuration" to "testResources")))
  implementation(libs.rhino)
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.cast.js"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.util"))
  testImplementation(libs.gson)
  testImplementation(libs.hamcrest)
  testImplementation(libs.junit)
  testImplementation(testFixtures(project(":com.ibm.wala.cast")))
  testImplementation(testFixtures(project(":com.ibm.wala.cast.js")))
  testFixturesImplementation(libs.junit)
  testFixturesImplementation(testFixtures(project(":com.ibm.wala.cast")))
  testFixturesImplementation(testFixtures(project(":com.ibm.wala.cast.js")))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") {
  environment("TRAVIS", 1)
  maxHeapSize = "800M"

  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    events("passed", "skipped", "failed")
  }

  if (gradle.startParameter.isOffline) exclude("**/FieldBasedJQueryTest.class")

  outputs.files(layout.buildDirectory.files("actual.dump", "expected.dump"))
}
