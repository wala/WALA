package com.ibm.wala.gradle

import org.gradle.plugins.ide.idea.model.IdeaModel

// Build configuration shared by all projects *except* for the root project.

plugins {
  idea
  id("com.ibm.wala.gradle.aggregated-javadoc")
  id("com.ibm.wala.gradle.project")
  id("de.undercouch.download")
}

version = rootProject.version

////////////////////////////////////////////////////////////////////////
//
//  Extra downloads pre-fetcher
//

tasks.register("downloads") {
  inputs.files(
      tasks.withType<VerifiedDownload>().matching {
        // not used in typical builds
        it.name != "downloadOcamlJava"
      })
}

////////////////////////////////////////////////////////////////////////
//
//  IntelliJ IDEA IDE integration
//

// workaround for <https://youtrack.jetbrains.com/issue/IDEA-140714>
the<IdeaModel>().module.excludeDirs.add(file("bin"))
