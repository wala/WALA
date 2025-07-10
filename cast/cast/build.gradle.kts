import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.addRpaths
import com.ibm.wala.gradle.cast.configure
import com.ibm.wala.gradle.cast.nativeLibraryOutput

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.subproject")
}

library {
  binaries.whenElementFinalized {
    compileTask.configure { macros["BUILD_CAST_DLL"] = "1" }

    this as CppSharedLibrary
    linkTask.addRpaths()
    linkTask.configure {
      if (targetMachine.operatingSystemFamily.isMacOs) {
        linkerArgs.add("-Wl,-install_name,@rpath/${nativeLibraryOutput.name}")
      }
      addJvmLibrary(this@whenElementFinalized)
    }
  }
}
