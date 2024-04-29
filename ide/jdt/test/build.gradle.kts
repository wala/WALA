plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

walaEclipseMavenCentral {
  testImplementation(
      "org.eclipse.core.contenttype",
      "org.eclipse.core.runtime",
      "org.eclipse.equinox.preferences",
      "org.eclipse.jdt.core",
      "org.eclipse.osgi",
  )
}

dependencies {
  testImplementation(libs.eclipse.osgi)
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
