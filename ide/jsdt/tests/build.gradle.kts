plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
}

walaEclipseMavenCentral {
  testImplementation(
      "org.eclipse.core.runtime",
      "org.eclipse.equinox.common",
      "org.eclipse.osgi",
  )
}

dependencies {
  testImplementation(libs.eclipse.osgi)
  testImplementation(libs.eclipse.wst.jsdt.core)
  testImplementation(libs.javax.annotation.api)
  testImplementation(projects.cast)
  testImplementation(projects.cast.js)
  testImplementation(projects.cast.js.rhino)
  testImplementation(projects.core)
  testImplementation(projects.ide.jsdt)
  testImplementation(projects.util)
  testImplementation(testFixtures(projects.ide.tests))
}

tasks.named<Test>("test") {
  // https://github.com/liblit/WALA/issues/5
  exclude("**/JSProjectScopeTest.class")
}
