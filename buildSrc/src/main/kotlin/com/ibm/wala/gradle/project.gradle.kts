package com.ibm.wala.gradle

import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.EclipseModel

// Build configuration shared by all projects *including* the root project.

plugins { eclipse }

////////////////////////////////////////////////////////////////////////
//
//  Eclipse IDE integration
//

// workaround for <https://github.com/gradle/gradle/issues/4802>
the<EclipseModel>().classpath.file.whenMerged {
  (this as Classpath).run {
    entries.forEach {
      if (it is AbstractClasspathEntry && it.entryAttributes["gradle_used_by_scope"] == "test")
          it.entryAttributes["test"] = true
    }
  }
}

////////////////////////////////////////////////////////////////////////
//
//  Helpers for dependency locking
//

// this task resolves dependencies in all sub-projects, making it easy to
// generate lockfiles
tasks.register<DependencyReportTask>("allDeps") {}
