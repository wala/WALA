import com.ibm.wala.gradle.cast.addJvmLibrary
import com.ibm.wala.gradle.cast.addRpaths
import com.ibm.wala.gradle.cast.configure

plugins {
  `cpp-library`
  id("com.ibm.wala.gradle.subproject")
}

library {
  binaries.whenElementFinalized {
    compileTask.configure { macros["BUILD_CAST_DLL"] = "1" }

    this as CppSharedLibrary
    addJvmLibrary(project)

    linkTask.addRpaths()
    linkTask.configure {
      if (targetMachine.operatingSystemFamily.isMacOs) {
        linkerArgs.add(provider { "-Wl,-install_name,@rpath/${linkedFile.get().asFile.name}" })
      }
    }
  }
}
