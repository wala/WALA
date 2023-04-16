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
  implementation(projects.cast)
  implementation(projects.cast.java)
  implementation(projects.cast.java.ecj)
  implementation(projects.core)
  implementation(projects.ide)
  implementation(projects.util)
}
