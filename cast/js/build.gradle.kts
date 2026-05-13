import com.ibm.wala.gradle.CreatePackageList
import com.ibm.wala.gradle.adHocDownload
import com.ibm.wala.gradle.dropTopDirectory

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  api(libs.jericho.html)
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
  testFixturesImplementation(testFixtures(projects.util))
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(testFixtures(projects.core))
}

val createPackageList =
    tasks.register<CreatePackageList>("createPackageList") { sourceSet(sourceSets.main.get()) }

val packageListDirectory =
    configurations.register("packageListDirectory") { isCanBeResolved = false }

val javadocDestinationDirectory =
    configurations.register("javadocDestinationDirectory") { isCanBeResolved = false }

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
      from({ tarTree(downloadAjaxslt.singleFile) })
      into(layout.buildDirectory.dir(name))
      dropTopDirectory()
    }

val processTestResources =
    tasks.named<Copy>("processTestResources") { from(unpackAjaxslt) { into("ajaxslt") } }

val testResources = configurations.register("testResources") { isCanBeResolved = false }

artifacts {
  add(javadocDestinationDirectory.name, tasks.javadoc)
  add(packageListDirectory.name, createPackageList)
  add(testResources.name, processTestResources)
}
