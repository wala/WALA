#!/usr/bin/env python

# script to build jars for maven central
import sys
import subprocess
import os

# action should be either 'install' (for local test)
# or 'deploy' (for deployment to maven central).
# if current version is SNAPSHOT, will only be deployed
# to sonatype's staging servers.  otherwise, will be
# deployed to maven central
action = sys.argv[1]

# projects for which we should build jars, in order
# will be prefixed with 'com.ibm.wala.'
projects = [
    "util",
    "shrike",
    "core",
    "cast",
    "cast.java",
    "cast.java.ecj",
    "cast.js",
    "cast.js.rhino",
    "dalvik",
    "scandroid"
    ]

for proj in projects:
    full_proj = "com.ibm.wala." + proj
    print full_proj
    os.chdir(full_proj)
    mvnCmd = "mvn -f mvncentral.xml clean " + action
    try:
        subprocess.check_output(mvnCmd, shell=True)
    except subprocess.CalledProcessError as e:
        print "OUTPUT"
        print e.output
        raise
    os.chdir("..")    

