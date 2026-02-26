import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.addRpaths

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.subproject")
}

val castHeaderDirectory by configurations.registering { isCanBeConsumed = false }

dependencies { castHeaderDirectory(project(":cast", "castHeaderDirectory")) }

library {
  privateHeaders.from(castHeaderDirectory)

  dependencies { implementation(projects.cast.cast) }

  binaries.whenElementFinalized {
    this as CppSharedLibrary
    addJvmLibrary(project)
    linkTask.addRpaths()
  }
}
