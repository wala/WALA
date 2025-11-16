import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.addRpaths
import com.ibm.wala.gradle.cast.configure
import com.ibm.wala.gradle.logToFile
import com.ibm.wala.gradle.valueToString
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.NATIVE_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.language.cpp.CppBinary.OPTIMIZED_ATTRIBUTE

plugins {
  `cpp-application`
  id("com.ibm.wala.gradle.subproject")
}

val coreResources by
    configurations.registering {
      isCanBeConsumed = false
      isTransitive = false
      attributes {
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class, RESOURCES))
      }
    }

val smokeMainExtraPathElements by
    configurations.registering {
      isCanBeConsumed = false
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
        attribute(USAGE_ATTRIBUTE, objects.named(Usage::class, NATIVE_RUNTIME))
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
        val checkSmokeMain by
            tasks.registering(Exec::class) {
              group = "verification"

              // main executable to run for test
              inputs.file(linkedFile)
              executable(linkedFile.valueToString)

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

              // all combined as a colon-delimited path list
              argumentProviders.add { listOf(pathElements.asPath) }

              // log output to file, although we don"t validate it
              logToFile(name)
            }

        if (!targetPlatform.get().operatingSystem.isWindows) {
          // Known to be broken on Windows, but not intentionally so.  Please fix if you
          // know how!  <https://github.com/wala/WALA/issues/608>
          tasks.named("check").configure { dependsOn(checkSmokeMain) }
        }
      }
    }
  }
}
