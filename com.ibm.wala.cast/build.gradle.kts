import org.gradle.api.attributes.Usage.NATIVE_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

val castCastSharedLibrary: Configuration by
    configurations.creating {
      isCanBeConsumed = false
      attributes {
        attribute(OPTIMIZED_ATTRIBUTE, false)
        attribute(USAGE_ATTRIBUTE, objects.named(Usage::class, NATIVE_RUNTIME))
      }
    }

val castJsJavadocDestinationDirectory: Configuration by
    configurations.creating { isCanBeConsumed = false }

val castJsPackageListDirectory: Configuration by configurations.creating { isCanBeConsumed = false }

val xlatorTestSharedLibrary: Configuration by
    configurations.creating {
      isCanBeConsumed = false
      isTransitive = false
      attributes {
        attribute(OPTIMIZED_ATTRIBUTE, false)
        attribute(USAGE_ATTRIBUTE, objects.named(Usage::class, NATIVE_RUNTIME))
      }
    }

dependencies {
  api(project(":com.ibm.wala.core")) {
    because("public method AstCGNode.addTarget receives an argument of type CGNode")
  }
  implementation(libs.commons.io)
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))
  castJsJavadocDestinationDirectory(
      project(
          mapOf(
              "path" to ":com.ibm.wala.cast.js", "configuration" to "javadocDestinationDirectory")))
  castCastSharedLibrary(project(":com.ibm.wala.cast:cast"))
  castJsPackageListDirectory(
      project(mapOf("path" to ":com.ibm.wala.cast.js", "configuration" to "packageListDirectory")))
  javadocClasspath(project(":com.ibm.wala.cast.js"))
  testImplementation(libs.junit)
  testRuntimeOnly(testFixtures(project(":com.ibm.wala.core")))
  xlatorTestSharedLibrary(project("xlator_test"))
}

val castHeaderDirectory: Configuration by configurations.creating { isCanBeResolved = false }

artifacts.add(
    castHeaderDirectory.name,
    tasks.named<JavaCompile>("compileTestJava").map { it.options.headerOutputDirectory })

tasks.named<Javadoc>("javadoc") {
  inputs.files(castJsPackageListDirectory)

  inputs.property("extdocURL", castJsJavadocDestinationDirectory.singleFile)
  inputs.property("packagelistLoc", castJsPackageListDirectory.singleFile)
  doFirst {
    (options as StandardJavadocDocletOptions).linksOffline(
        inputs.properties["extdocURL"].toString(), inputs.properties["packagelistLoc"].toString())
  }
}

tasks.named<Test>("test") {
  inputs.files(xlatorTestSharedLibrary)
  doFirst { systemProperty("java.library.path", xlatorTestSharedLibrary.singleFile.parent) }

  if (rootProject.extra["isWindows"] as Boolean) {

    // Windows has nothing akin to RPATH for embedding DLL search paths in other DLLs or
    // executables.  Instead, we need to ensure that any required DLLs are in the standard
    // executable search path at test run time.
    //
    // Unfortunately, Windows environment variables are case-insensitive.  So we cannot simply
    // append the DLL's path to `$PATH`.  Rather, we need to append to an environment variable whose
    // name is case-insensitively equal to `"path"`, whether that's `$PATH`, `$Path`, `$path`, etc.

    inputs.files(castCastSharedLibrary)
    val pathEntry = environment.entries.find { it.key.equals("path", true) }!!
    environment(pathEntry.key, "${pathEntry.value};${castCastSharedLibrary.singleFile.parent}")
  }
}
