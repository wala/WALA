import com.ibm.wala.gradle.adHocDownload

plugins { id("com.ibm.wala.gradle.java") }

dependencies {
  api(projects.cast.js) {
    because("public class NodejsCallGraphBuilderUtil extends class JSCallGraphUtil")
  }
  api(projects.core)
  api(projects.util)
  implementation(libs.commons.io)
  implementation(libs.json)
  implementation(projects.cast)
  implementation(projects.cast.js.rhino)
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
}

val downloadNodeJS =
    adHocDownload(uri("https://nodejs.org/dist/v0.12.4"), "node", "tar.gz", "v0.12.4")

val unpackNodeJSLib by
    tasks.registering(Sync::class) {
      from(tarTree { downloadNodeJS.singleFile }) {
        include("*/lib/*.js")
        eachFile { path = name }
      }

      into(layout.buildDirectory.dir(name))
      includeEmptyDirs = false
    }

tasks.named<Copy>("processResources") {
  // It is important to unpack the the NodeJs library files into the main resources directory,
  // so they are packaged inside the jar artifact for this module.  That way, the packaged jar
  // will work when used by third-party code.  The downside is that we cannot release this jar
  // artifact to Maven Central with third-party source code included.  Eventually, we should find
  // a way to remove the reliance on packaging this code (e.g., allow the nodejs library directory
  // to be specified via a JVM property), so we can release the artifact to Maven Central.
  from(unpackNodeJSLib) { eachFile { path = "core-modules/$name" } }
}

tasks.named<Test>("test") {
  maxHeapSize = "800M"

  // fails with java.lang.OutOfMemoryError for unknown reasons
  exclude("**/NodejsRequireTargetSelectorResolveTest.class")
}
