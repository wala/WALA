import com.ibm.wala.gradle.cast.addCastLibrary

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.subproject")
}

val castHeaderDirectory by configurations.registering { isCanBeConsumed = false }

dependencies {
  castHeaderDirectory(project(mapOf("path" to ":cast", "configuration" to "castHeaderDirectory")))
}

library {
  privateHeaders.from(castHeaderDirectory)

  dependencies { implementation(projects.cast.cast) }

  binaries.whenElementFinalized {
    this as CppSharedLibrary
    linkTask.get().addCastLibrary(this)
  }
}
