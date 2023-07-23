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
  testFixturesApi("org.eclipse.core.resources", "org.eclipse.core.runtime")
  testFixturesImplementation("org.eclipse.ui.ide")
  testImplementation("org.eclipse.jface")
}

val coreTestDataJar: Configuration by
    configurations.creating {
      isCanBeConsumed = false
      isTransitive = false
    }

val coreTestResources: Configuration by configurations.creating { isCanBeConsumed = false }

val coreMainSource: Configuration by
    configurations.creating {
      isCanBeConsumed = false
      attributes {
        attribute(VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType::class, MAIN_SOURCES))
      }
    }

val ifdsExplorerExampleClasspath: Configuration by
    configurations.creating {
      isCanBeConsumed = false
      isTransitive = false
    }

dependencies {
  coreMainSource(project(mapOf("path" to ":core")))
  coreTestDataJar(projects.core)
  coreTestResources(project(mapOf("path" to ":core", "configuration" to "testResources")))
  ifdsExplorerExampleClasspath(sourceSets.test.map { it.runtimeClasspath })
  ifdsExplorerExampleClasspath(
      project(mapOf("path" to ":core", "configuration" to "collectTestDataJar")))
  testImplementation(projects.core)
  testImplementation(projects.ide)
  testImplementation(projects.util)
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("org.eclipse.platform:org.eclipse.osgi.services"))
        .using(module(libs.eclipse.osgi.get().toString()))
        .because(
            "both provide several of the same classes, but org.eclipse.osgi includes everything we need from both")
    substitute(module("xml-apis:xml-apis-ext"))
        .using(module(libs.w3c.css.sac.get().toString()))
        .because(
            "both provide several of the same classes, but org.w3c.css.sac includes everything we need from both")
  }
}

tasks.named<Test>("test") {
  if (System.getProperty("os.name").startsWith("Mac OS X")) {
    // Required for running SWT code
    jvmArgs = listOf("-XstartOnFirstThread")
  }
}

// This is required for IFDSExplorereExample to work correctly
tasks.named<Copy>("processTestResources") {
  from(coreTestDataJar)
  from(coreTestResources) { include("wala.testdata.txt") }
}

// Task to make it easier to run IFDSExplorerExample.  Command-line arguments are passed via
// the "args" Gradle project property, e.g. (on a Mac):
// `./gradlew :com.ibm.wala.ide.tests:runIFDSExplorerExample -Pargs="-dotExe /usr/local/bin/dot
// -viewerExe /usr/bin/open"`
tasks.register<JavaExec>("runIFDSExplorerExample") {
  group = "Execution"
  description = "Run the IFDSExplorerExample driver"
  classpath = ifdsExplorerExampleClasspath
  mainClass = "com.ibm.wala.examples.drivers.IFDSExplorerExample"
  if (System.getProperty("os.name").startsWith("Mac OS X")) {
    jvmArgs = listOf("-XstartOnFirstThread")
  }
  project.findProperty("args")?.let { args((it as String).split("\\s+".toRegex())) }
}
