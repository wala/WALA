import com.ibm.wala.gradle.cast.addCastLibrary

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.cast.native")
  id("com.ibm.wala.gradle.subproject")
}

val castHeaderDirectory: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  castHeaderDirectory(project(mapOf("path" to ":cast", "configuration" to "castHeaderDirectory")))
}

library {
  privateHeaders.from(castHeaderDirectory)

  dependencies { implementation(projects.cast.cast) }

  binaries.whenElementFinalized {
    addCastLibrary(this, (this as CppSharedLibrary).linkTask.get(), project)
  }
}
