////////////////////////////////////////////////////////////////////////
//
//  plugin configuration must precede everything else
//

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.JavaVersion.VERSION_17

buildscript { dependencies.classpath(libs.commons.io) }

plugins {
  idea
  java
  alias(libs.plugins.dependency.analysis)
  alias(libs.plugins.file.lister)
  alias(libs.plugins.node)
  alias(libs.plugins.shellcheck)
  alias(libs.plugins.task.tree)
  alias(libs.plugins.version.catalog.update)
  alias(libs.plugins.versions)
  id("com.ibm.wala.gradle.javadoc")
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("com.ibm.wala.gradle.project")
}

repositories {
  // to get the google-java-format jar and dependencies
  mavenCentral()
}

JavaVersion.current().let {
  if (!it.isCompatibleWith(VERSION_17)) {
    logger.error(
        "Gradle is running on a Java $it JVM, which is not compatible with Java 17. Build failures are likely. For advice on changing JVMs, visit <https://docs.gradle.org/current/userguide/build_environment.html> and look for discussion of the `org.gradle.java.home` Gradle property or the `JAVA_HOME` environment variable.")
  }
}

////////////////////////////////////////////////////////////////////////
//
//  common Java setup shared by multiple projects
//

group = name

version = property("VERSION_NAME") as String

///////////////////////////////////////////////////////////////////////
//
//  Javadoc documentation
//

val aggregatedJavadocClasspath by configurations.registering { isCanBeConsumed = false }

val aggregatedJavadocSource by configurations.registering { isCanBeConsumed = false }

dependencies {
  subprojects {
    pluginManager.withPlugin("java-base") {
      aggregatedJavadocClasspath(
          project(mapOf("path" to path, "configuration" to "javadocClasspath")))

      aggregatedJavadocSource(project(mapOf("path" to path, "configuration" to "javadocSource")))
    }
  }
}

tasks.register<Javadoc>("aggregatedJavadocs") {
  description = "Generate javadocs from all child projects as if they were a single project"
  group = "Documentation"
  destinationDir = layout.buildDirectory.dir("docs/javadoc").get().asFile
  title = "${project.name} $version API"
  (options as StandardJavadocDocletOptions).author(true)
  classpath = aggregatedJavadocClasspath.get()
  source(aggregatedJavadocSource)
}

////////////////////////////////////////////////////////////////////////
//
//  linters for various specific languages or file formats
//

// Gradle dependencies
dependencyAnalysis.issues { all { onAny { severity("fail") } } }

// shell scripts, provided they have ".sh" extension
shellcheck {
  isUseDocker = false
  shellcheckBinary = "shellcheck"
  sourceFiles =
      fileTree(".") {
        exclude("**/build")
        include("**/*.sh")
      }
}

// Markdown
val lintMarkdown by
    tasks.registering(NpxTask::class) {
      group = "verification"
      command = "markdownlint-cli2"
      val markdownFiles = fileTree(".") { include("*.md") }
      inputs.files(markdownFiles)
      inputs.file(".markdownlint-cli2.yaml")
      args = markdownFiles.map { it.path }
      outputs.file(layout.buildDirectory.file("$name.stamp"))
      doLast { outputs.files.singleFile.createNewFile() }
    }

tasks.named("check") { dependsOn("buildHealth", lintMarkdown) }

tasks.named("shellcheck") { group = "verification" }

// install Java reformatter as git pre-commit hook
tasks.register<Copy>("installGitHooks") {
  from("config/hooks/pre-commit-stub")
  rename { "pre-commit" }
  into(".git/hooks")
  filePermissions {
    listOf(user, group, other).forEach {
      it.read = true
      it.write = true
      it.execute = true
    }
  }
}

listOf("check", "spotlessCheck", "spotlessApply").forEach {
  tasks.named(it) { dependsOn(gradle.includedBuild("build-logic").task(":$it")) }
}

////////////////////////////////////////////////////////////////////////
//
//  Run IntelliJ IDEA inspections on entire project tree
//
//  We don"t make `check` depend on `checkInspectionResults` for two
//  reasons.  First, `runInspections` is quite slow.  Second,
//  `runInspections` cannot run while the same user account is running a
//  regular, graphical instance of IntelliJ IDEA.  These limitations
//  make `runInspections` and `checkInspectionResults` more suitable for
//  use in CI/CD pipelines than for daily use by live WALA developers.
//

val runInspections by
    tasks.registering(Exec::class) {
      group = "intellij-idea"
      description = "Run all enabled IntelliJ IDEA inspections on the entire WALA project"

      val ideaDir = file("$rootDir/.idea")
      inputs.dir("$ideaDir/scopes")

      val inspectionProfile = file("$ideaDir/inspectionProfiles/No_Back_Sliding.xml")
      inputs.file(inspectionProfile)

      val textResultsFile = layout.buildDirectory.file("$name.txt").get().asFile
      outputs.file(textResultsFile)

      // Inspections examine a wide variety of files, not just Java
      // sources, so this task is out-of-date if nearly any other file has
      // changed.
      inputs.files(fileLister.obtainPartialFileTree())

      executable = findProperty("runInspections.IntelliJ-IDEA.command") as String? ?: "idea"
      args("inspect", rootDir, inspectionProfile, textResultsFile, "-v1", "-format", "plain")

      // The `idea` command above always fails with an
      // `IllegalArgumentException` arising from
      // `PlainTextFormatter.getPath`.  Fortunately, this only happens
      // *after* `idea` has already written out the results file.  So we
      // should ignore that command"s exit value, and only fail this task
      // if the results file is missing.
      isIgnoreExitValue = true
      doLast {
        if (!textResultsFile.exists()) {
          throw GradleException("IntelliJ IDEA command failed without creating $textResultsFile.")
        }
      }
    }

tasks.register("checkInspectionResults") {
  group = "intellij-idea"
  description = "Fail if any IntelliJ IDEA inspections produced errors or warnings"

  inputs.files(runInspections)
  doFirst {
    var failed = false
    val problemPattern = "\\[(ERROR|WARNING)]".toRegex()
    inputs.files.singleFile.forEachLine {
      if (problemPattern.matches(it)) {
        failed = true
        println(it)
      }
    }
    if (failed) {
      throw GradleException(
          "One or more IntelliJ IDEA inspections failed.  See logged problems above, or \"${inputs.files.singleFile}\" for full details.  WEAK WARNINGs are allowed, but all ERRORs and WARNINGs must be corrected.")
    }
  }

  val stampFile = layout.buildDirectory.file("$name.stamp")
  outputs.file(stampFile)
  doLast { stampFile.get().asFile.createNewFile() }
}

////////////////////////////////////////////////////////////////////////
//
//  Check for updated dependencies
//

tasks.withType<DependencyUpdatesTask> {
  gradleReleaseChannel = "current"
  rejectVersionIf {
    candidate.run {

      // Determine what an unwanted version looks like, if any, based on the dependency group.
      val unwantedVersionPattern =
          when (group) {

            // Apache Commons IO snapshot releases have timestamped versions like `20030203.000550`.
            // We only want stable releases, which have versions like `2.11.0`.
            "commons-io" -> "\\d+\\.\\d+"

            // AssertJ milestone releases have versions with milestone numbers like `4.0.0-M1`. We
            // only want stable releases, which have versions like `4.0.0`.
            "org.assertj" -> ".*-M\\d+"

            // JUnit milestone releases have versions with milestone numbers like `5.11.0-M2`. We
            // only want stable releases, which have versions like `5.10.2`.
            "org.junit",
            "org.junit.jupiter",
            "org.junit.platform",
            "org.junit.vintage" -> ".*-M\\d+"

            // SLF4J alpha releases have versions with alpha numbers like `2.1.0-alpha1`. We only
            // want stable releases, which have versions like `2.0.13`.
            "org.slf4j" -> ".*-alpha\\d+"

            // Assume that anything not excluded above is OK.
            else -> null
          }

      // Reject certain versions, if a rejection pattern was provided. Otherwise assume that any
      // version is OK.
      when (unwantedVersionPattern) {
        null -> false
        else -> version.matches(unwantedVersionPattern.toRegex())
      }
    }
  }
}
