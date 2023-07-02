plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

walaEclipseMavenCentral {
  api("org.eclipse.equinox.common")
  implementation(
      "org.eclipse.core.jobs",
      "org.eclipse.core.resources",
      "org.eclipse.core.runtime",
      "org.eclipse.equinox.app",
      "org.eclipse.jdt.core",
      "org.eclipse.jface",
      "org.eclipse.osgi",
      "org.eclipse.ui.workbench",
  )
}

dependencies {
  implementation(projects.cast)
  implementation(projects.cast.java)
  implementation(projects.cast.java.ecj)
  implementation(projects.core)
  implementation(projects.ide)
  implementation(projects.util)
}
