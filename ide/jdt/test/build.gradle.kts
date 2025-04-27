plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

walaEclipseMavenCentral {
  testImplementation(
      "org.eclipse.core.runtime",
  )
}

dependencies {
  testImplementation(libs.assertj.core)
  testImplementation(libs.eclipse.osgi)
  testImplementation(libs.osgi.framework)
  testImplementation(projects.cast.java)
  testImplementation(projects.core)
  testImplementation(projects.ide.jdt)
  testImplementation(projects.util)
  testImplementation(testFixtures(projects.cast.java))
  testImplementation(testFixtures(projects.ide.tests))
}

tasks.named<Test>("test") {
  maxHeapSize = "1200M"
  workingDir = project(":cast:java:test:data").projectDir
}
