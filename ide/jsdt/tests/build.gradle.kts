plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
}

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    listOf(
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.common",
            "org.eclipse.osgi",
        )
        .forEach { dep("testImplementation", it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  testImplementation(libs.eclipse.osgi)
  testImplementation(libs.eclipse.wst.jsdt.core)
  testImplementation(libs.javax.annotation.api)
  testImplementation(libs.junit)
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
