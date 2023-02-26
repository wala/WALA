@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    api("org.eclipse.pde.core")
    listOf(
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
        .forEach { implementation(it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
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
