@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
}

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    api("org.eclipse.equinox.common")
    listOf(
            "org.eclipse.core.jobs",
            "org.eclipse.core.resources",
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.app",
            "org.eclipse.jdt.core",
            "org.eclipse.jface",
            "org.eclipse.osgi",
            "org.eclipse.ui.workbench",
        )
        .forEach { implementation(it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.cast.java"))
  implementation(project(":com.ibm.wala.cast.java.ecj"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.ide"))
  implementation(project(":com.ibm.wala.util"))
}
