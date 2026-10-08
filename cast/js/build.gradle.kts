import com.ibm.wala.gradle.CreatePackageList
import com.ibm.wala.gradle.adHocDownload
import com.ibm.wala.gradle.dropTopDirectory
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.DOCUMENTATION
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.Category.VERIFICATION
import org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import org.gradle.api.attributes.DocsType.JAVADOC
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

walaPublishing.pomName = "WALA CAst JavaScript"

dependencies {
  api(libs.jericho.html)
  api(libs.jspecify)
  api(projects.cast) { because("public class JSCallGraphUtil extends class CAstCallGraphUtil") }
  api(projects.core)
  api(projects.util)
  implementation(libs.commons.io)
  implementation(libs.gson)
  implementation(projects.shrike)
  javadocClasspath(projects.cast.js.rhino)
  testFixturesApi(libs.junit.jupiter.api)
  testFixturesApi(projects.cast)
  testFixturesApi(projects.core)
  testFixturesApi(projects.util)
  testFixturesApi(testFixtures(projects.cast))
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.jetbrains.annotations)
  testFixturesImplementation(testFixtures(projects.util))
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(testFixtures(projects.core))
}

val createPackageList =
    tasks.register<CreatePackageList>("createPackageList") {
      description = "Generate package list for Javadoc cross-reference"
      sourceRoots.from(sourceSets.main.map { it.java.srcDirs })
    }

val packageListDirectory =
    configurations.register("packageListDirectory") {
      isCanBeResolved = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(RESOURCES))
      }
    }

val javadocDestinationDirectory =
    configurations.register("javadocDestinationDirectory") {
      isCanBeResolved = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(DOCUMENTATION))
        attribute(DOCS_TYPE_ATTRIBUTE, named(JAVADOC))
      }
    }

tasks.named<Test>("test") { maxHeapSize = "800M" }

val downloadAjaxslt =
    adHocDownload(
        uri(
            "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/ajaxslt"
        ),
        "ajaxslt",
        "tar.gz",
        "0.8.1",
    )

val unpackAjaxslt =
    tasks.register<Sync>("unpackAjaxslt") {
      description = "Unpack AJAXSLT test resources"
      from(tarTree(downloadAjaxslt))
      into(layout.buildDirectory.dir(name))
      dropTopDirectory()
    }

val processTestResources =
    tasks.named<Copy>("processTestResources") { from(unpackAjaxslt) { into("ajaxslt") } }

val testResources =
    configurations.register("testResources") {
      isCanBeResolved = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(RESOURCES))
      }
    }

artifacts {
  add(javadocDestinationDirectory.name, tasks.javadoc)
  add(packageListDirectory.name, createPackageList)
  add(testResources.name, processTestResources)
}
