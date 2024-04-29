plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

walaEclipseMavenCentral {
  api("org.eclipse.pde.core")
  implementation(
      "org.eclipse.core.commands",
      "org.eclipse.core.jobs",
      "org.eclipse.core.resources",
      "org.eclipse.core.runtime",
      "org.eclipse.equinox.common",
      "org.eclipse.jdt.core",
      "org.eclipse.jface",
      "org.eclipse.osgi",
      "org.eclipse.swt",
      "org.eclipse.ui.workbench",
  )
}

dependencies {
  implementation(projects.core)
  implementation(projects.util)
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("xml-apis:xml-apis-ext"))
        .using(module(libs.w3c.css.sac.get().toString()))
        .because(
            "both provide several of the same classes, but org.w3c.css.sac includes everything we need from both")
  }
}
