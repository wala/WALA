import os
import sys
import subprocess
from pathlib import Path

MODULES = {
  "com.ibm.wala.util":[
    "com.ibm.wala.cast.java.ecj",
    "com.ibm.wala.cast.java",
    "com.ibm.wala.cast.js.nodejs",
    "com.ibm.wala.cast.js.rhino",
    "com.ibm.wala.cast.js.rhino",
    "com.ibm.wala.cast.js",
    "com.ibm.wala.cast",
    "com.ibm.wala.core",
    "com.ibm.wala.dalvik",
    "com.ibm.wala.ide.jdt",
    "com.ibm.wala.ide.jsdt",
    "com.ibm.wala.ide",
    "com.ibm.wala.scandroid",
    "com.ibm.wala.shrike"
  ]
}
args = []
target = "com.ibm.wala.util"
core = "~/.m2/repository/edu/ucr/cs/riple/nullawayannotator/core/1.3.4-SNAPSHOT"
downstream_enabled = True
repo_path = Path(os.getcwd()).parent.absolute()
build_command = "cd {} && ANNOTATOR_TARGET={} ./gradlew {}:compileJava --rerun-tasks".format(repo_path, target, target)
downstream_build_command = "cd {} && ANNOTATOR_TARGET={} ./gradlew {} --rerun-tasks".format(repo_path, target, ' '.join(["{}:compileJava".format(dep) for dep in MODULES[target]]))
nullaway_library_model_loader = "{}/com.ibm.wala.librarymodelsloader/src/main/resources/com/ibm/wala/librarymodelsloader/nullable-methods.tsv".format(repo_path)
out_dir = "/tmp/NullAwayFix"
config_paths = ["{}\t{}\n".format("{}/{}/config/nullaway.xml".format(repo_path, dep), "{}/{}/config/scanner.xml".format(repo_path, dep)) for dep in MODULES[target]]
print(config_paths)
paths = "/tmp/NullAwayFix/paths.tsv"
f = open(paths, "w")
f.writelines(config_paths)

args += ["-bc", build_command]
args += ["-cp", paths]
args += ["-i", "com.ibm.wala.Initializer"]
args += ["-d", "5"]
args += ["-n", "com.sun.istack.internal.Nullable"]

if downstream_enabled:
  args += ["-adda"]
  args += ["-nlmlp", nullaway_library_model_loader]
  args += ["-ddbc", downstream_build_command]

print(args)

subprocess.Popen(["cd", core] + ["java", "-jar", "core-1.3.4-SNAPSHOT.jar"] + args)

