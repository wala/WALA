plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
}

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    listOf(
            "org.eclipse.core.jobs",
            "org.eclipse.core.resources",
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.common",
            "org.eclipse.osgi",
            "org.eclipse.ui.workbench",
        )
        .forEach { implementation(it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  api(projects.ide) { because("public class JavaScriptHeadlessUtil extends class HeadlessUtil") }
  implementation(libs.eclipse.wst.jsdt.core)
  implementation(libs.eclipse.wst.jsdt.ui)
  implementation(libs.javax.annotation.api)
  implementation(projects.cast)
  implementation(projects.cast.js)
  implementation(projects.cast.js.rhino)
  implementation(projects.core)
  implementation(projects.util)
  testFixturesImplementation(libs.javax.annotation.api)
}
