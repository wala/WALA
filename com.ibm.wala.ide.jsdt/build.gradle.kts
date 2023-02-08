@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
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
  api(project(":com.ibm.wala.ide")) {
    because("public class JavaScriptHeadlessUtil extends class HeadlessUtil")
  }
  implementation(libs.eclipse.wst.jsdt.core)
  implementation(libs.eclipse.wst.jsdt.ui)
  implementation(libs.javax.annotation.api)
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.cast.js"))
  implementation(project(":com.ibm.wala.cast.js.rhino"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.util"))
  testFixturesImplementation(libs.javax.annotation.api)
}
