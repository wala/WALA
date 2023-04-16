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
  testImplementation(projects.cast)
  testImplementation(projects.cast.java)
  testImplementation(projects.cast.java.ecj)
  testImplementation(projects.core)
  testImplementation(projects.ide)
  testImplementation(projects.ide.jdt)
  testImplementation(projects.shrike)
  testImplementation(projects.util)
  testImplementation(testFixtures(projects.cast.java))
  testImplementation(testFixtures(projects.ide.tests))
}

tasks.named<Test>("test") {
  maxHeapSize = "1200M"
  workingDir = project(":cast:java:test:data").projectDir
}
