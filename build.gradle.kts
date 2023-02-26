////////////////////////////////////////////////////////////////////////
//
//  plugin configuration must precede everything else
//

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript { dependencies.classpath(libs.commons.io) }

@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
  idea
  java
  alias(libs.plugins.file.lister)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shellcheck)
  alias(libs.plugins.task.tree)
  alias(libs.plugins.versions)
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.javadoc")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("com.ibm.wala.gradle.project")
}

repositories {
  // to get the google-java-format jar and dependencies
  mavenCentral()
}

val osName: String by extra(System.getProperty("os.name"))
val archName: String by extra(System.getProperty("os.arch"))
val isWindows by extra(osName.startsWith("Windows "))

JavaVersion.current().let {
  if (!it.isJava11Compatible) {
    logger.error(
        "Gradle is running on a Java $it JVM, which is not compatible with Java 11. Build failures are likely. For advice on changing JVMs, visit <https://docs.gradle.org/current/userguide/build_environment.html> and look for discussion of the `org.gradle.java.home` Gradle property or the `JAVA_HOME` environment variable.")
  }
}

////////////////////////////////////////////////////////////////////////
//
//  common Java setup shared by multiple projects
//

group = name

version = properties["VERSION_NAME"] as String

// version of Eclipse JARs to use for Eclipse-integrated WALA components.
val eclipseVersion: String by extra(libs.versions.eclipse.asProvider()::get)

///////////////////////////////////////////////////////////////////////
//
//  Javadoc documentation
//

val aggregatedJavadocClasspath: Configuration by configurations.creating { isCanBeConsumed = false }

val aggregatedJavadocSource: Configuration by configurations.creating { isCanBeConsumed = false }

eclipseMavenCentral { release(eclipseVersion) { useNativesForRunningPlatform() } }

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
  setDestinationDir(file("$buildDir/docs/javadoc"))
  title = "${project.name} $version API"
  (options as StandardJavadocDocletOptions).author(true)
  classpath = aggregatedJavadocClasspath
  source(aggregatedJavadocSource)
}

////////////////////////////////////////////////////////////////////////
//
//  linters for various specific languages or file formats
//

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

val shellcheckTask = tasks.named("shellcheck") { group = "verification" }

// install Java reformatter as git pre-commit hook
tasks.register<Copy>("installGitHooks") {
  from("config/hooks/pre-commit-stub")
  rename { "pre-commit" }
  into(".git/hooks")
  fileMode = 0b111_111_111
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

      val textResultsFile = file("$buildDir/${name}.txt")
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

  val stampFile = file("$buildDir/${name}.stamp")
  outputs.file(stampFile)
  doLast { stampFile.createNewFile() }
}

////////////////////////////////////////////////////////////////////////
//
//  Check for updated dependencies
//

tasks.withType<DependencyUpdatesTask>().configureEach {
  gradleReleaseChannel = "current"
  rejectVersionIf {
    candidate.run {
      // Apache Commons IO snapshot releases have timestamped versions like `20030203.000550`. We
      // don't want these. We only want stable releases, which have versions like `2.11.0`.
      group == "commons-io" && module == "commons-io" && version.matches("\\d+\\.\\d+".toRegex())
    }
  }
}
