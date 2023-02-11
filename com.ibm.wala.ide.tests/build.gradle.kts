import org.gradle.api.attributes.VerificationType.MAIN_SOURCES
import org.gradle.api.attributes.VerificationType.VERIFICATION_TYPE_ATTRIBUTE

@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
  `java-library`
  `java-test-fixtures`
  id("com.ibm.wala.gradle.java")
  id("com.diffplug.eclipse.mavencentral")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    dep("testFixturesApi", "org.eclipse.core.resources")
    dep("testFixturesApi", "org.eclipse.core.runtime")
    dep("testFixturesImplementation", "org.eclipse.ui.ide")
    dep("testImplementation", "org.eclipse.jface")
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
  testImplementation(libs.junit)
  testImplementation(project(":com.ibm.wala.core"))
  testImplementation(project(":com.ibm.wala.ide"))
  testImplementation(project(":com.ibm.wala.util"))
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
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass.set("com.ibm.wala.examples.drivers.IFDSExplorerExample")
  if (System.getProperty("os.name").startsWith("Mac OS X")) {
    jvmArgs = listOf("-XstartOnFirstThread")
  }
  project.findProperty("args")?.let { args((it as String).split("\\s+".toRegex())) }
}
