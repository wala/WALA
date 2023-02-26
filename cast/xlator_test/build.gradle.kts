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
  // Temporary change to build on M1 Mac machines, until
  // https://github.com/gradle/gradle/issues/18876
  // is fixed
  if (rootProject.extra["osName"] == "Mac OS X" && rootProject.extra["archName"] == "aarch64") {
    targetMachines.add(machines.macOS.x86_64)
  }
  privateHeaders.from(castHeaderDirectory)

  dependencies { implementation(projects.cast.cast) }

  binaries.whenElementFinalized {
    addCastLibrary(this, (this as CppSharedLibrary).linkTask.get(), project)
  }
}
