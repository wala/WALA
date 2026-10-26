import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.VERIFICATION
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES

plugins { id("com.ibm.wala.gradle.java") }

val extraTestResources =
    configurations.register("extraTestResources") {
      isCanBeConsumed = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(RESOURCES))
      }
    }

dependencies {
  api(libs.jspecify)
  api(projects.cast.js)
  extraTestResources(project(":cast:js"))
  implementation(libs.htmlparser)
  implementation(projects.cast)
  implementation(projects.util)
  testImplementation(testFixtures(projects.cast.js.rhino))
}

tasks.named<Copy>("processTestResources") { from(extraTestResources) }

tasks.named<Test>("test") { maxHeapSize = "800M" }
