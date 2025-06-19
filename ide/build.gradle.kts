plugins {
  alias(libs.plugins.dependency.analysis)
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

walaEclipseMavenCentral {
  api(
      "org.eclipse.core.resources",
      "org.eclipse.core.runtime",
      "org.eclipse.equinox.common",
      "org.eclipse.jface",
      "org.eclipse.osgi",
      "org.eclipse.pde.core",
  )
  implementation(
      "org.eclipse.jdt.core",
      "org.eclipse.ui.workbench",
  )
}

dependencies {
  api(libs.osgi.framework)
  api(projects.core)
  api(projects.util)
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("xml-apis:xml-apis-ext"))
        .using(module(libs.w3c.css.sac.get().toString()))
        .because(
            "both provide several of the same classes, but org.w3c.css.sac includes everything we need from both")
  }
}

dependencyAnalysis.issues {
  onDuplicateClassWarnings {
    exclude(
        "org.osgi.framework.Bundle",
        "org.osgi.framework.BundleContext",
    )
  }
  onIncorrectConfiguration { exclude("org.eclipse.pde:org.eclipse.pde.core") }
  onUsedTransitiveDependencies {
    exclude(
        "org.eclipse.platform:org.eclipse.swt.cocoa.macosx.aarch64",
        "org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64",
        "org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64",
    )
  }
}
