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
    (linkTask as Provider<out LinkSharedLibrary>).configure {
      if (targetMachine.operatingSystemFamily.isMacOs) {
        // NOTE: intentionally `provider { ...get() }`, not `linkedFile.map { }`:
        // `linkedFile` is this task's own output, so `map` would make the task
        // depend on itself (circular). The `.get()` here runs at execution time
        // when linker args are needed, not during configuration, so it is allowed.
        linkerArgs.add(provider { "-Wl,-install_name,@rpath/${linkedFile.get().asFile.name}" })
      }
    }
  }
}
