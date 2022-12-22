plugins {
  application
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    listOf(
            "org.eclipse.core.runtime",
            "org.eclipse.jdt.core",
        )
        .forEach { implementation(it) }
    constrainTransitivesToThisRelease()
  }
}

val runSourceDirectory: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  implementation(
      project(":com.ibm.wala.cast"),
  )
  implementation(project(":com.ibm.wala.cast.java"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))
  runSourceDirectory(
      project(
          mapOf(
              "path" to ":com.ibm.wala.cast.java.test.data",
              "configuration" to "testJavaSourceDirectory")))
  testImplementation(
      "junit:junit:4.13.2",
  )
  testImplementation(testFixtures(project(":com.ibm.wala.cast.java")))
  testRuntimeOnly(testFixtures(project(":com.ibm.wala.core")))
}

application.mainClass.set("com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph")

val run by
    tasks.existing(JavaExec::class) {
      // this is for testing purposes
      args =
          listOf(
              "-sourceDir", runSourceDirectory.files.single().toString(), "-mainClass", "LArray1")

      // log output to file, although we don"t validate it
      val outFile = project.layout.buildDirectory.file("SourceDirCallGraph.log")
      outputs.file(outFile)
      doFirst {
        outFile.get().asFile.outputStream().let {
          standardOutput = it
          errorOutput = it
        }
      }
    }

tasks.named<Test>("test") {
  maxHeapSize = "1200M"

  workingDir(project(":com.ibm.wala.cast.java.test.data").projectDir)

  // ensure the command-line driver for running ECJ works
  dependsOn(run)
}
