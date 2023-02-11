@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
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
  testImplementation(project(":com.ibm.wala.cast"))
  testImplementation(project(":com.ibm.wala.cast.js"))
  testImplementation(project(":com.ibm.wala.cast.js.rhino"))
  testImplementation(project(":com.ibm.wala.core"))
  testImplementation(project(":com.ibm.wala.ide.jsdt"))
  testImplementation(project(":com.ibm.wala.util"))
  testImplementation(testFixtures(project(":com.ibm.wala.ide.tests")))
}

tasks.named<Test>("test") {
  // https://github.com/liblit/WALA/issues/5
  exclude("**/JSProjectScopeTest.class")
}
