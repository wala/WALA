import com.ibm.wala.gradle.VerifiedDownload

plugins { id("com.ibm.wala.gradle.java") }

dependencies {
  api(project(":com.ibm.wala.cast.js")) {
    because("public class NodejsCallGraphBuilderUtil extends class JSCallGraphUtil")
  }
  implementation(libs.commons.io)
  implementation(libs.json)
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.cast.js.rhino"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.util"))
  testImplementation(libs.junit)
  testRuntimeOnly(testFixtures(project(":com.ibm.wala.core")))
}

val downloadNodeJS by
    tasks.registering(VerifiedDownload::class) {
      src("https://nodejs.org/dist/v0.12.4/node-v0.12.4.tar.gz")
      dest(project.layout.buildDirectory.file("nodejs.tar.gz"))
      algorithm("SHA-1")
      checksum("147ff79947752399b870fcf3f1fc37102100b545")
    }

val unpackNodeJSLib by
    tasks.registering(Copy::class) {
      from(downloadNodeJS.map { tarTree(it.dest) }) {
        include("*/lib/*.js")
        eachFile { path = name }
      }

      into(layout.buildDirectory.dir(name))
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
