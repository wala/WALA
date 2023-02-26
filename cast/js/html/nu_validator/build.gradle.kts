plugins { id("com.ibm.wala.gradle.java") }

val extraTestResources: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  extraTestResources(project(mapOf("path" to ":cast:js", "configuration" to "testResources")))
  implementation(libs.htmlparser)
  implementation(projects.cast)
  implementation(projects.cast.js)
  implementation(projects.util)
  testImplementation(testFixtures(projects.cast))
  testImplementation(testFixtures(projects.cast.js))
  testImplementation(testFixtures(projects.cast.js.rhino))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") { maxHeapSize = "800M" }
