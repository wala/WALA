import com.ibm.wala.gradle.forEachJavaProject
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.Category.VERIFICATION
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.VerificationType.MAIN_SOURCES
import org.gradle.api.attributes.VerificationType.VERIFICATION_TYPE_ATTRIBUTE

// Generates the WALA API documentation as if every Java project were a single project.
//
// This logic lives in its own project rather than in the root so that the root can stay free of the
// `java` plugin. Resolving this project's classpath requires resolving `:ide`'s runtime classpath,
// and therefore SWT, whose Maven coordinates contain the `${osgi.platform}` placeholder. That
// placeholder resolves only via `com.ibm.wala.gradle.eclipse-maven-central`, which applies the Java
// toolchain to the project it is applied to. Confining it to this project keeps it off the root.

plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.javadoc")
}

group = rootProject.group

version = rootProject.version

val aggregatedJavadocClasspathExtras =
    configurations.register("aggregatedJavadocClasspathExtras") {
      isCanBeConsumed = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val aggregatedJavadocRuntimeElements =
    configurations.register("aggregatedJavadocRuntimeElements") {
      isCanBeConsumed = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val aggregatedJavadocSource =
    configurations.register("aggregatedJavadocSource") {
      isCanBeConsumed = false
      // Source dirs only: transitive dependencies would inherit these attributes and fail
      // against external modules, which declare no verification variants.
      isTransitive = false
      attributes {
        attribute(BUNDLING_ATTRIBUTE, named(EXTERNAL))
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(VERIFICATION_TYPE_ATTRIBUTE, named(MAIN_SOURCES))
      }
    }

dependencies {
  // Some `compileOnly` dependencies are needed during Javadoc generation but are not included in
  // `aggregatedJavadocRuntimeElements`.
  aggregatedJavadocClasspathExtras(libs.jetbrains.annotations)
  aggregatedJavadocClasspathExtras(libs.nullaway.annotations)

  forEachJavaProject {
    aggregatedJavadocRuntimeElements(project(it.path, "runtimeElements"))
    aggregatedJavadocSource(project(it.path, "mainSourceElements"))
  }
}

tasks.register<Javadoc>("aggregatedJavadocs") {
  description = "Generate javadocs from all child projects as if they were a single project"
  group = "Documentation"
  destinationDirectory = layout.buildDirectory.dir("docs/javadoc")
  title = "${rootProject.name} $version API"
  (options as StandardJavadocDocletOptions).author(true)
  classpath = files(aggregatedJavadocClasspathExtras, aggregatedJavadocRuntimeElements)
  source(aggregatedJavadocSource)
  include("**/*.java")
}
