import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.addRpaths
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.Usage.C_PLUS_PLUS_API
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.subproject")
}

val castHeaderDirectory =
    configurations.register("castHeaderDirectory") {
      isCanBeConsumed = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(USAGE_ATTRIBUTE, named(C_PLUS_PLUS_API))
      }
    }

dependencies { castHeaderDirectory(project(":cast")) }

library {
  privateHeaders.from(castHeaderDirectory)

  dependencies { implementation(projects.cast.cast) }

  binaries.whenElementFinalized {
    this as CppSharedLibrary
    addJvmLibrary(project)
    linkTask.addRpaths()
  }
}
