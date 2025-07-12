plugins {
  application
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

walaEclipseMavenCentral {
  implementation(
      "org.eclipse.equinox.common",
      "org.eclipse.jdt.core",
  )
}

val runSourceDirectory by configurations.registering { isCanBeConsumed = false }

dependencies {
  implementation(libs.eclipse.ecj)
  implementation(projects.cast)
  implementation(projects.cast.java)
  implementation(projects.core)
  implementation(projects.shrike)
  implementation(projects.util)
  runSourceDirectory(
      project(
          mapOf("path" to ":cast:java:test:data", "configuration" to "testJavaSourceDirectory")))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(testFixtures(projects.cast.java))
}

application.mainClass = "com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph"

val run by
    tasks.existing(JavaExec::class) {
      val runSourceDirectoryPath = runSourceDirectory.map { it.files.single().toString() }
      // this is for testing purposes
      argumentProviders.add {
        listOf("-sourceDir", runSourceDirectoryPath.get(), "-mainClass", "LArray1")
      }

      // log output to file, although we don"t validate it
      val outFile = layout.buildDirectory.file("SourceDirCallGraph.log")
      outputs.file(outFile)
      doFirst {
        outFile.get().asFile.outputStream().let {
          standardOutput = it
          errorOutput = it
        }
      }
    }

// ensure the command-line driver for running ECJ works
tasks.named("check") { dependsOn(run) }

tasks.named<Test>("test") {
  maxHeapSize = "1200M"

  workingDir(project(":cast:java:test:data").projectDir)
}
