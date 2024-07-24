plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

walaEclipseMavenCentral {
  api(
      "org.eclipse.core.resources",
      "org.eclipse.equinox.app",
      "org.eclipse.equinox.common",
      "org.eclipse.jdt.core",
      "org.eclipse.jface",
      "org.eclipse.osgi",
      "org.eclipse.ui.workbench",
  )
  implementation(
      "org.eclipse.core.jobs",
  )
}

dependencies {
  api(libs.osgi.framework)
  api(projects.cast)
  api(projects.cast.java)
  api(projects.cast.java.ecj)
  api(projects.core)
  api(projects.ide)
  api(projects.util)
  implementation(libs.eclipse.ecj)
}
