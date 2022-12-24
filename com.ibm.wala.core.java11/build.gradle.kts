import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

sourceSets.create("testSubjects")

dependencies {
  implementation(project(":com.ibm.wala.core"))
  testImplementation(libs.junit)
  testImplementation(
      testFixtures(project(":com.ibm.wala.core")),
  )
  testRuntimeOnly(sourceSets["testSubjects"].output.classesDirs)
}

////////////////////////////////////////////////////////////////////////
//
//  collect "com.ibm.wala.core.java11.testdata_1.0.0.jar"
//

val collectTestData by
    tasks.registering(Jar::class) {
      archiveFileName.set("com.ibm.wala.core.java11.testdata_1.0.0.jar")
      from(tasks.named("compileTestSubjectsJava"))
      includeEmptyDirs = false
      destinationDirectory.set(layout.buildDirectory.dir(name))
    }

////////////////////////////////////////////////////////////////////////

tasks.withType<JavaCompile>().configureEach { options.release.set(null as Int?) }

tasks.named<Copy>("processTestResources") { from(collectTestData) }

tasks.named<Test>("test") {
  maxHeapSize = "1500M"
  systemProperty("com.ibm.wala.junit.profile", "short")
  // classpath += files project(":com.ibm.wala.core.java11").sourceSets.test.java.outputDir
  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    events("passed", "skipped", "failed")
  }

  outputs.file(layout.buildDirectory.file("report"))
}
