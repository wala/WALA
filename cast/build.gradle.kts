import org.gradle.api.attributes.Usage.NATIVE_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.operating-system")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

val castCastSharedLibrary by
    configurations.registering {
      isCanBeConsumed = false
      attributes {
        attribute(OPTIMIZED_ATTRIBUTE, false)
        attribute(USAGE_ATTRIBUTE, objects.named(Usage::class, NATIVE_RUNTIME))
      }
    }

val castJsJavadocDestinationDirectory by configurations.registering { isCanBeConsumed = false }

val castJsPackageListDirectory by configurations.registering { isCanBeConsumed = false }

val xlatorTestSharedLibrary by
    configurations.registering {
      isCanBeConsumed = false
      isTransitive = false
      attributes {
        attribute(OPTIMIZED_ATTRIBUTE, false)
        attribute(USAGE_ATTRIBUTE, objects.named(Usage::class, NATIVE_RUNTIME))
      }
    }

dependencies {
  api(projects.core) {
    because("public method AstCGNode.addTarget receives an argument of type CGNode")
  }
  api(projects.shrike)
  api(projects.util)
  implementation(libs.commons.io)
  castJsJavadocDestinationDirectory(
      project(mapOf("path" to ":cast:js", "configuration" to "javadocDestinationDirectory")))
  castCastSharedLibrary(projects.cast.cast)
  castJsPackageListDirectory(
      project(mapOf("path" to ":cast:js", "configuration" to "packageListDirectory")))
  javadocClasspath(projects.cast.js)
  testFixturesApi(projects.core)
  testFixturesImplementation(projects.util)
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
  xlatorTestSharedLibrary(projects.cast.xlatorTest)
}

val castHeaderDirectory by configurations.registering { isCanBeResolved = false }

artifacts.add(
    castHeaderDirectory.name,
    tasks.named<JavaCompile>("compileTestJava").map { it.options.headerOutputDirectory })

tasks.named<Javadoc>("javadoc") {
  inputs.files(castJsPackageListDirectory)

  val extdocURL = castJsJavadocDestinationDirectory.map { it.singleFile }
  val packagelistLoc = castJsPackageListDirectory.map { it.singleFile }
  inputs.property("extdocURL", extdocURL)
  inputs.property("packagelistLoc", packagelistLoc)

  doFirst {
    (options as StandardJavadocDocletOptions).linksOffline(
        extdocURL.get().toString(), packagelistLoc.get().toString())
  }
}

tasks.named<Test>("test") {
  inputs.files(xlatorTestSharedLibrary)
  val xlatorTestSharedLibraryDir = xlatorTestSharedLibrary.map { it.singleFile.parent }
  doFirst { systemProperty("java.library.path", xlatorTestSharedLibraryDir.get()) }

  if (project.extra["isWindows"] as Boolean) {

    // Windows has nothing akin to RPATH for embedding DLL search paths in other DLLs or
    // executables.  Instead, we need to ensure that any required DLLs are in the standard
    // executable search path at test run time.
    //
    // Unfortunately, Windows environment variables are case-insensitive.  So we cannot simply
    // append the DLL's path to `$PATH`.  Rather, we need to append to an environment variable whose
    // name is case-insensitively equal to `"path"`, whether that's `$PATH`, `$Path`, `$path`, etc.

    inputs.files(castCastSharedLibrary)
    val pathEntry = environment.entries.find { it.key.equals("path", true) }!!
    val castCastSharedLibraryDir = castCastSharedLibrary.map { it.singleFile.parent }
    doFirst { environment(pathEntry.key, "${pathEntry.value};${castCastSharedLibraryDir.get()}") }
  }
}
