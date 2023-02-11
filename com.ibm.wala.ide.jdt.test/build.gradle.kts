@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
}

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    listOf(
            "org.eclipse.core.contenttype",
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.preferences",
            "org.eclipse.jdt.core",
            "org.eclipse.osgi",
        )
        .forEach { dep("testImplementation", it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  testImplementation(libs.eclipse.osgi)
  testImplementation(libs.junit)
  testImplementation(project(":com.ibm.wala.cast"))
  testImplementation(project(":com.ibm.wala.cast.java"))
  testImplementation(project(":com.ibm.wala.cast.java.ecj"))
  testImplementation(project(":com.ibm.wala.core"))
  testImplementation(project(":com.ibm.wala.ide"))
  testImplementation(project(":com.ibm.wala.ide.jdt"))
  testImplementation(project(":com.ibm.wala.shrike"))
  testImplementation(project(":com.ibm.wala.util"))
  testImplementation(testFixtures(project(":com.ibm.wala.cast.java")))
  testImplementation(testFixtures(project(":com.ibm.wala.ide.tests")))
}

tasks.named<Test>("test") {
  maxHeapSize = "1200M"
  workingDir = project(":com.ibm.wala.cast.java.test.data").projectDir
}
