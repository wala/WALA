plugins { id("com.ibm.wala.gradle.java") }

val extraTestResources by configurations.registering { isCanBeConsumed = false }

dependencies {
  api(projects.cast.js)
  extraTestResources(project(mapOf("path" to ":cast:js", "configuration" to "testResources")))
  implementation(libs.htmlparser)
  implementation(projects.cast)
  implementation(projects.util)
  testImplementation(testFixtures(projects.cast.js.rhino))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") { maxHeapSize = "800M" }
