import com.ibm.wala.gradle.cast.addCastLibrary
import com.ibm.wala.gradle.cast.addRpath
import com.ibm.wala.gradle.cast.configure
import org.gradle.api.attributes.LibraryElements.CLASSES
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES
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
      isTransitive = false
      attributes {
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements::class, CLASSES))
      }
    }

fun createXlatorConfig(isOptimized: Boolean): NamedDomainObjectProvider<Configuration> =
    configurations.register(
        "xlatorTest${if (isOptimized) "Release" else "Debug"}SharedLibraryConfig") {
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
    smokeMainExtraPathElements(projects.cast)
    smokeMainExtraPathElements(projects.core)
    smokeMainExtraPathElements(projects.util)
    implementation(projects.cast.cast)
    xlatorTestDebugSharedLibraryConfig(projects.cast.xlatorTest)
    xlatorTestReleaseSharedLibraryConfig(projects.cast.xlatorTest)
  }

  binaries.whenElementFinalized {
    this as CppExecutable
    linkTask.configure {
      val libxlatorTestConfig =
          if (isOptimized) xlatorTestReleaseSharedLibraryConfig
          else xlatorTestDebugSharedLibraryConfig
      val libxlatorTest = libxlatorTestConfig.map { it.singleFile }
      addRpath(libxlatorTest)
      addCastLibrary(this@whenElementFinalized)

      if (isDebuggable && !isOptimized) {
        val checkSmokeMain by
            tasks.registering(Exec::class) {

              // main executable to run for test
              inputs.file(linkedFile)
              executable(
                  object {
                    val toString by lazy { linkedFile.get().asFile.toString() }

                    override fun toString() = toString
                  })

              // xlator Java bytecode + implementation of native methods
              inputs.files(libxlatorTestConfig)
              val pathElements = project.objects.listProperty<File>()
              pathElements.addAll(
                  files("../build/classes/java/test", libxlatorTest.map { it.parent }))

              // "primordial.txt" resource loaded during test
              pathElements.add(coreResources.map { it.singleFile })
              inputs.files(coreResources)

              // additional supporting Java class files
              inputs.files(smokeMainExtraPathElements)
              pathElements.addAll(smokeMainExtraPathElements)

              // all combined as a colon-delimited path list
              argumentProviders.add { listOf(pathElements.get().joinToString(":")) }

              // log output to file, although we don"t validate it
              val outFile = layout.buildDirectory.file("${name}.log")
              outputs.file(outFile)
              doFirst {
                outFile.get().asFile.outputStream().let {
                  standardOutput = it
                  errorOutput = it
                }
              }
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
