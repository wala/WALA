import com.ibm.wala.gradle.CreatePackageList
import com.ibm.wala.gradle.VerifiedDownload
import java.net.URL

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  api(project(":com.ibm.wala.cast")) {
    because("public class JSCallGraphUtil extends class CAstCallGraphUtil")
  }
  implementation(libs.commons.io)
  implementation(libs.gson)
  implementation(libs.jericho.html)
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))
  javadocClasspath(project(":com.ibm.wala.cast.js.rhino"))
  testFixturesImplementation(libs.junit)
  testFixturesImplementation(testFixtures(project(":com.ibm.wala.cast")))
  testFixturesImplementation(testFixtures(project(":com.ibm.wala.core")))
  testImplementation(libs.junit)
  testImplementation(testFixtures(project(":com.ibm.wala.cast")))
  testImplementation(testFixtures(project(":com.ibm.wala.core")))
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
      src.set(
          URL(
              "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/ajaxslt/$versionedArchive"))
      dest.set(project.layout.buildDirectory.file(versionedArchive))
      checksum.set("c995abe3310a401bb4db7f28a6409756")
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
  val java: JavaPluginExtension by extensions
  add(javadocDestinationDirectory.name, file("${java.docsDir}/javadoc"))
  add(packageListDirectory.name, createPackageList)
  add(testResources.name, processTestResources)
}
