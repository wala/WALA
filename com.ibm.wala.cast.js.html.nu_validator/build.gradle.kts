plugins { id("com.ibm.wala.gradle.java") }

val extraTestResources: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  extraTestResources(
      project(mapOf("path" to ":com.ibm.wala.cast.js", "configuration" to "testResources")))
  implementation("nu.validator.htmlparser:htmlparser:1.4")
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.cast.js"))
  implementation(project(":com.ibm.wala.util"))
  testImplementation(testFixtures(project(":com.ibm.wala.cast")))
  testImplementation(testFixtures(project(":com.ibm.wala.cast.js")))
  testImplementation(testFixtures(project(":com.ibm.wala.cast.js.rhino")))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") { maxHeapSize = "800M" }
