import org.gradle.api.attributes.VerificationType.MAIN_SOURCES
import org.gradle.api.attributes.VerificationType.VERIFICATION_TYPE_ATTRIBUTE

plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.github.hauner.jarTest") version "1.0.1"
  id("com.ibm.wala.gradle.java")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    listOf(
            "org.eclipse.core.commands",
            "org.eclipse.core.jobs",
            "org.eclipse.core.resources",
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.common",
            "org.eclipse.jface",
            "org.eclipse.osgi",
            "org.eclipse.ui.ide",
        )
        .forEach { dep("testImplementation", it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
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

dependencies {
  coreMainSource(project(mapOf("path" to ":com.ibm.wala.core")))
  coreTestDataJar(project(":com.ibm.wala.core"))
  coreTestResources(
      project(mapOf("path" to ":com.ibm.wala.core", "configuration" to "testResources")))
  testImplementation("org.eclipse.platform:org.eclipse.osgi:3.17.0")
  testImplementation(project(":com.ibm.wala.core"))
  testImplementation(project(":com.ibm.wala.ide"))
  testImplementation(project(":com.ibm.wala.util"))
  testImplementation("org.eclipse.platform:org.eclipse.ui.workbench") {
    version { strictly("3.120.0") }
  }
  testImplementation("junit:junit:4.13.2")
  testImplementation(
      "org.hamcrest:hamcrest:2.2",
  )
  testRuntimeOnly(testFixtures(project(":com.ibm.wala.core")))
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("org.eclipse.platform:org.eclipse.osgi.services"))
        .using(module("org.eclipse.platform:org.eclipse.osgi:3.17.0"))
        .because(
            "both provide several of the same classes, but org.eclipse.osgi includes everything we need from both")
    substitute(module("xml-apis:xml-apis-ext"))
        .using(module("org.eclipse.birt.runtime:org.w3c.css.sac:1.3.1.v200903091627"))
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
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass.set("com.ibm.wala.examples.drivers.IFDSExplorerExample")
  if (System.getProperty("os.name").startsWith("Mac OS X")) {
    jvmArgs = listOf("-XstartOnFirstThread")
  }
  project.findProperty("args")?.let { args((it as String).split("\\s+".toRegex())) }
}
