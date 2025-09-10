package com.ibm.wala.gradle

// Build configuration shared by all projects *except* for the root project.

plugins {
  idea
  id("com.ibm.wala.gradle.project")
}

version = rootProject.version

////////////////////////////////////////////////////////////////////////
//
//  IntelliJ IDEA IDE integration
//

// workaround for <https://youtrack.jetbrains.com/issue/IDEA-140714>
idea.module.excludeDirs.add(file("bin"))
