import com.ibm.wala.gradle.CreatePackageList
import com.ibm.wala.gradle.VerifiedDownload
import java.net.URI

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  api(projects.cast) { because("public class JSCallGraphUtil extends class CAstCallGraphUtil") }
  implementation(libs.commons.io)
  implementation(libs.gson)
  implementation(libs.jericho.html)
  implementation(projects.core)
  implementation(projects.shrike)
  implementation(projects.util)
  javadocClasspath(projects.cast.js.rhino)
  testFixturesImplementation(testFixtures(projects.cast))
  testImplementation(testFixtures(projects.cast))
  testImplementation(testFixtures(projects.core))
}

val createPackageList by
    tasks.registering(CreatePackageList::class) { sourceSet(sourceSets.main.get()) }

val packageListDirectory: Configuration by configurations.creating { isCanBeResolved = false }

val javadocDestinationDirectory: Configuration by
    configurations.creating { isCanBeResolved = false }

tasks.named<Test>("test") { maxHeapSize = "800M" }

val downloadAjaxslt by
    tasks.registering(VerifiedDownload::class) {
      val version = "0.8.1"
      val versionedArchive = "ajaxslt-${version}.tar.gz"
      src =
          URI(
              "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/ajaxslt/$versionedArchive")
      dest = project.layout.buildDirectory.file(versionedArchive)
      checksum = "c995abe3310a401bb4db7f28a6409756"
    }

val unpackAjaxslt by
    tasks.registering(Sync::class) {
      from(downloadAjaxslt.map { tarTree(it.dest) }) {
        eachFile {
          val newSegments = relativePath.segments.drop(1).toTypedArray()
          relativePath = RelativePath(!isDirectory, *newSegments)
        }
      }
      into(project.layout.buildDirectory.dir(name))
    }

val processTestResources by tasks.existing(Copy::class) { from(unpackAjaxslt) { into("ajaxslt") } }

val testResources: Configuration by configurations.creating { isCanBeResolved = false }

artifacts {
  add(javadocDestinationDirectory.name, tasks.javadoc)
  add(packageListDirectory.name, createPackageList)
  add(testResources.name, processTestResources)
}
