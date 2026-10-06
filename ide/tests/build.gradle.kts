import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.Category.VERIFICATION
import org.gradle.api.attributes.LibraryElements.CLASSES
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES
import org.gradle.api.attributes.VerificationType.MAIN_SOURCES
import org.gradle.api.attributes.VerificationType.VERIFICATION_TYPE_ATTRIBUTE

plugins {
  `java-library`
  `java-test-fixtures`
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

walaEclipseMavenCentral {
  testFixturesApi(
      "org.eclipse.core.resources",
      "org.eclipse.core.runtime",
      "org.eclipse.equinox.common",
      "org.eclipse.ui.ide",
  )
  testFixturesImplementation("org.eclipse.ui.workbench")
  testImplementation("org.eclipse.jface")
}

val coreTestDataJar =
    configurations.register("coreTestDataJar") {
      isCanBeConsumed = false
      isTransitive = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val coreTestResources =
    configurations.register("coreTestResources") {
      isCanBeConsumed = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(RESOURCES))
      }
    }

val coreMainSource =
    configurations.register("coreMainSource") {
      isCanBeConsumed = false
      attributes { attribute(VERIFICATION_TYPE_ATTRIBUTE, named(MAIN_SOURCES)) }
    }

val ifdsExplorerExampleClasspath =
    configurations.register("ifdsExplorerExampleClasspath") {
      isCanBeConsumed = false
      isTransitive = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(CLASSES))
      }
    }

dependencies {
  coreMainSource(project(":core"))
  coreTestDataJar(projects.core)
  coreTestResources(project(":core"))
  ifdsExplorerExampleClasspath(sourceSets.test.map { it.runtimeClasspath })
  // Keeps its explicit configuration name: this merges plain files with a project variant,
  // which no single attribute request can express.
  ifdsExplorerExampleClasspath(project(":core", "collectTestDataJar"))
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(libs.eclipse.osgi)
  testImplementation(libs.eclipse.osgi)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.osgi.framework)
  testImplementation(projects.core)
  testImplementation(projects.ide)
  testImplementation(projects.util)
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("org.eclipse.platform:org.eclipse.osgi.services"))
        .using(module(libs.eclipse.osgi.get().toString()))
        .because(
            "both provide several of the same classes, but org.eclipse.osgi includes everything we need from both"
        )
  }
}

tasks.named<Test>("test") {
  if (System.getProperty("os.name").startsWith("Mac OS X")) {
    // Required for running SWT code
    jvmArgs = listOf("-XstartOnFirstThread")
  }
}

// This is required for IFDSExplorerExample to work correctly
tasks.named<Copy>("processTestResources") {
  from(coreTestDataJar)
  from(coreTestResources) { include("wala.testdata.txt") }
}

// Task to make it easier to run IFDSExplorerExample.  Command-line arguments are passed via
// the "args" Gradle project property, e.g., (on a Mac):
// `./gradlew :com.ibm.wala.ide.tests:runIFDSExplorerExample -Pargs="-dotExe /usr/local/bin/dot
// -viewerExe /usr/bin/open"`
tasks.register<JavaExec>("runIFDSExplorerExample") {
  group = "Execution"
  description = "Run the IFDSExplorerExample driver"
  classpath(ifdsExplorerExampleClasspath)
  mainClass = "com.ibm.wala.examples.drivers.IFDSExplorerExample"
  if (System.getProperty("os.name").startsWith("Mac OS X")) {
    jvmArgs = listOf("-XstartOnFirstThread")
  }
  providers.gradleProperty("args").orNull?.let { args(it.split("\\s+".toRegex())) }
}
