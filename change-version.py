#!/usr/bin/env python

# specify versions as version number, optionally followed by
# '-SNAPSHOT' for a snapshot version
import sys
import subprocess

def runAndPrint(cmd):
    print cmd
    subprocess.check_output(cmd, shell=True)

oldVersion = sys.argv[1]
newVersion = sys.argv[2]

print oldVersion + " --> " + newVersion

oldVersion = oldVersion.replace(".", "\.")
newVersion = newVersion.replace(".", "\.")

runAndPrint("mvn clean -q")

runAndPrint("find -E ./ -regex \".*(pom|mvncentral)\.xml\" | xargs -n 1 perl -pi -e \'s/" + oldVersion + "/" + newVersion + "/g\'")

runAndPrint("perl -pi -e \'s/" + oldVersion + "/" + newVersion + "/g\' build.gradle")

oldIsSnapshot = oldVersion.endswith("SNAPSHOT")
newIsSnapShot = newVersion.endswith("SNAPSHOT")

bundleOld = oldVersion if not oldIsSnapshot else oldVersion.replace("-SNAPSHOT","\.qualifier")
bundleNew = newVersion if not newIsSnapShot else newVersion.replace("-SNAPSHOT","\.qualifier")

runAndPrint("find -E ./ -regex \".*(MANIFEST\.MF|feature\.xml)\" | xargs -n 1 perl -pi -e \'s/" + bundleOld + "/" + bundleNew + "/g\'")

print "done"
    

