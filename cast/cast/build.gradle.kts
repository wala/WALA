import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.nativeLibraryOutput

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.cast.native")
  id("com.ibm.wala.gradle.subproject")
}

library {
  binaries.whenElementFinalized {
    compileTask.get().configure(closureOf<CppCompile> { macros["BUILD_CAST_DLL"] = "1" })

    this as CppSharedLibrary
    linkTask
        .get()
        .configure(
            closureOf<LinkSharedLibrary> {
              if (targetMachine.operatingSystemFamily.isMacOs) {
                linkerArgs.append("-Wl,-install_name,@rpath/${nativeLibraryOutput.name}")
              }
              addJvmLibrary(this@whenElementFinalized, this, project)
            })
  }
}
