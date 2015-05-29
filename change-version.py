#!/usr/bin/env python

# specify versions as version number, optionally followed by
# '-SNAPSHOT' for a snapshot version
import sys
import subprocess

oldVersion = sys.argv[1]
newVersion = sys.argv[2]

print oldVersion + " --> " + newVersion

oldVersion = oldVersion.replace(".", "\.")
newVersion = newVersion.replace(".", "\.")

cleanCmd = "mvn clean -q"
print cleanCmd
subprocess.check_output(cleanCmd, shell=True)

xmlCmd = "find -E ./ -regex \".*(pom|mvncentral)\.xml\" | xargs -n 1 perl -pi -e \'s/" + oldVersion + "/" + newVersion + "/g\'"
print xmlCmd
subprocess.check_output(xmlCmd, shell=True)

oldIsSnapshot = oldVersion.endswith("SNAPSHOT")
newIsSnapShot = newVersion.endswith("SNAPSHOT")

bundleOld = oldVersion if not oldIsSnapshot else oldVersion.replace("-SNAPSHOT","\.qualifier")
bundleNew = newVersion if not newIsSnapShot else newVersion.replace("-SNAPSHOT","\.qualifier")

otherCmd = "find -E ./ -regex \".*(MANIFEST\.MF|feature\.xml)\" | xargs -n 1 perl -pi -e \'s/" + bundleOld + "/" + bundleNew + "/g\'"

print otherCmd
subprocess.check_output(otherCmd, shell=True)

print "done"
    

