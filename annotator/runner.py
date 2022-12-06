import os
import sys
import subprocess
from pathlib import Path
import shutil
import tempfile

MODULES = {
  "com.ibm.wala.util": [
    "com.ibm.wala.cast.java.ecj",
    "com.ibm.wala.cast.java",
    "com.ibm.wala.cast.js.nodejs",
    "com.ibm.wala.cast.js.rhino",
    "com.ibm.wala.cast.js",
    "com.ibm.wala.cast",
    "com.ibm.wala.core",
    "com.ibm.wala.dalvik",
    "com.ibm.wala.ide.jdt",
    "com.ibm.wala.ide",
    "com.ibm.wala.scandroid",
    "com.ibm.wala.shrike"
  ]
}

ANNOTATIONS = {
  "NULLABLE": "org.jspecify.annotations.Nullable",
  "INITIALIZER": "com.ibm.wala.Initializer",
  "NULLUNMARKED": "org.jspecify.annotations.NullUnmarked"
}

verbose = False
downstream_enabled = False

args = []
target = "com.ibm.wala.util"
core = "{}/.m2/repository/edu/ucr/cs/riple/annotator/annotator-core/1.3.4".format(
    Path.home())
repo_path = Path(os.getcwd()).parent.absolute()
build_command = "cd {} && ANNOTATOR_TARGET={} ./gradlew :{}:compileJava --rerun-tasks".format(
    repo_path, target, target)
downstream_build_command = "cd {} && ANNOTATOR_TARGET={} ./gradlew {} --rerun-tasks".format(
    repo_path, target,
    ' '.join([":{}:compileJava".format(dep) for dep in MODULES[target]]))
nullaway_library_model_loader = "{}/com.ibm.wala.librarymodelsloader/src/main/resources/com/ibm/wala/librarymodelsloader/nullable-methods.tsv".format(
    repo_path)

# create tmp dir
out_dir = tempfile.TemporaryDirectory().name
os.mkdir(out_dir)

config_dirs = ["{}/{}/config".format(repo_path, module) for module in
               [target] + MODULES[target]]
# make dirs
for dir in config_dirs:
  if os.path.exists(dir):
    shutil.rmtree(dir)
  os.mkdir(dir)

# write config paths
config_paths = [
  "{}\t{}\n".format("{}/nullaway.xml".format(config_path),
                    "{}/scanner.xml".format(config_path)) for config_path in
  config_dirs]
paths = os.path.join(out_dir, "paths.tsv")
f = open(paths, "w")
f.writelines(config_paths)
f.close()

# make args
args += ["-bc", build_command]
args += ["-cp", paths]
args += ["-i", ANNOTATIONS['INITIALIZER']]
args += ["-depth", "5"]
args += ["-d", out_dir]
args += ["-n", ANNOTATIONS['NULLABLE']]
args += ["-fr", ANNOTATIONS['NULLUNMARKED']]
args += ["-am", "strict"]

if downstream_enabled:
  args += ["-adda"]
  args += ["-nlmlp", nullaway_library_model_loader]
  args += ["-ddbc", downstream_build_command]

if verbose:
  args += ["-rboserr"]

print(args)
os.chdir(core)
p = subprocess.Popen(["java", "-jar", "annotator-core-1.3.4.jar"] + args)
p.communicate()

# cleanup
for dir in config_dirs:
  shutil.rmtree(dir)
shutil.rmtree(out_dir)
