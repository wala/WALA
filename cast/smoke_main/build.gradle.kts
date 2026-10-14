import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.addRpaths
import com.ibm.wala.gradle.cast.configure
import com.ibm.wala.gradle.cast.resolveJvmRuntimeDirectories
import com.ibm.wala.gradle.logToFile
import com.ibm.wala.gradle.valueToString
import java.io.File
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.NATIVE_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE

plugins {
  `cpp-application`
  id("com.ibm.wala.gradle.subproject")
}

val coreResources =
    configurations.register("coreResources") {
      isCanBeConsumed = false
      isTransitive = false
      attributes { attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(RESOURCES)) }
    }

val smokeMainExtraPathElements =
    configurations.register("smokeMainExtraPathElements") {
      isCanBeConsumed = false
      attributes { attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, named(STANDARD_JVM)) }
      attributes.attribute(USAGE_ATTRIBUTE, objects.named(Usage::class, JAVA_RUNTIME))
    }

fun createXlatorConfig(isOptimized: Boolean): NamedDomainObjectProvider<Configuration> =
    configurations.register(
        "xlatorTest${if (isOptimized) "Release" else "Debug"}SharedLibraryConfig"
    ) {
      isCanBeConsumed = false
      isTransitive = false
      attributes {
        attribute(OPTIMIZED_ATTRIBUTE, isOptimized)
        attribute(USAGE_ATTRIBUTE, named(NATIVE_RUNTIME))
      }
    }

val xlatorTestDebugSharedLibraryConfig = createXlatorConfig(false)

val xlatorTestReleaseSharedLibraryConfig = createXlatorConfig(true)

application {
  dependencies {
    coreResources(projects.core)
    implementation(projects.cast.cast)
    smokeMainExtraPathElements(testFixtures(projects.cast))
    xlatorTestDebugSharedLibraryConfig(projects.cast.xlatorTest)
    xlatorTestReleaseSharedLibraryConfig(projects.cast.xlatorTest)
  }

  binaries.whenElementFinalized {
    this as CppExecutable

    addJvmLibrary(project)

    linkTask.addRpaths()
    (linkTask as Provider<out LinkExecutable>).configure {
      val libxlatorTestConfig =
          if (isOptimized) xlatorTestReleaseSharedLibraryConfig
          else xlatorTestDebugSharedLibraryConfig
      val libxlatorTest = libxlatorTestConfig.map { it.singleFile }

      if (isDebuggable && !isOptimized) {
        val checkSmokeMain =
            tasks.register<Exec>("checkSmokeMain") {
              description = "Run the native smoke test executable"
              group = "verification"

              // main executable to run for test
              inputs.file(linkedFile)
              executable(linkedFile.valueToString)

              // The Windows loader has no analogue of an rpath: it searches only the directory
              // holding the executable, then `PATH`.  Nothing copies `cast.dll` beside
              // `smoke_main.exe`, and the `jvm.dll` behind the `jvm.lib` we link against lives in
              // yet another directory, so the executable would not even start.  Name all of them
              // on `PATH`.  Elsewhere `addRpaths` already encodes these locations, so
              // `resolveJvmRuntimeDirectories` yields nothing; prepending the linked libraries
              // there is inert, as neither the macOS nor the Linux loader consults `PATH` for
              // shared libraries.
              val jvmRuntimeDirectories =
                  providers.provider {
                    resolveJvmRuntimeDirectories(
                        File(providers.systemProperty("java.home").get()),
                        targetMachine.operatingSystemFamily.name,
                    )
                  }
              // the parent directory of each shared library we link against, such as `cast.dll`
              val linkedLibraryDirectories =
                  providers.provider { libs.map { library -> library.parentFile } }
              val inheritedPath = providers.environmentVariable("PATH")
              val searchPath =
                  providers.provider {
                    listOf(
                            files(jvmRuntimeDirectories, linkedLibraryDirectories).asPath,
                            inheritedPath.orNull,
                        )
                        .filterNot { it.isNullOrEmpty() }
                        .joinToString(File.pathSeparator)
                  }
              inputs.files(jvmRuntimeDirectories, linkedLibraryDirectories)
              environment("PATH", searchPath.valueToString)

              // xlator Java bytecode + implementation of native methods
              inputs.files(libxlatorTestConfig)
              val pathElements =
                  files("../build/classes/java/test", libxlatorTest.map { it.parent })

              // "primordial.txt" resource loaded during test
              inputs.files(coreResources)
              pathElements.from(coreResources)

              // additional supporting Java class files
              inputs.files(smokeMainExtraPathElements)
              pathElements.from(smokeMainExtraPathElements)

              // all combined as a path list delimited by the platform's separator
              argumentProviders.add { listOf(pathElements.asPath) }

              // log output to file, although we don"t validate it
              logToFile(name)
            }

        tasks.named("check").configure { dependsOn(checkSmokeMain) }
      }
    }
  }
}
