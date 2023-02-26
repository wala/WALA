import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.nativeLibraryOutput

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.cast.native")
  id("com.ibm.wala.gradle.subproject")
}

library {
  // Temporary change to build on M1 Mac machines, until
  // https://github.com/gradle/gradle/issues/18876
  // is fixed
  if (rootProject.extra["osName"] == "Mac OS X" && rootProject.extra["archName"] == "aarch64") {
    targetMachines.add(machines.macOS.x86_64)
  }

  binaries.whenElementFinalized {
    compileTask.get().configure(closureOf<CppCompile> { macros["BUILD_CAST_DLL"] = "1" })

    this as CppSharedLibrary
    linkTask
        .get()
        .configure(
            closureOf<LinkSharedLibrary> {
              if (targetMachine.operatingSystemFamily.isMacOs) {
                linkerArgs.add("-Wl,-install_name,@rpath/${nativeLibraryOutput.name}")
              }
              addJvmLibrary(this@whenElementFinalized, this, project)
            })
  }
}
