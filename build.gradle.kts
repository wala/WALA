////////////////////////////////////////////////////////////////////////
//
//  plugin configuration must precede everything else
//

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.gradle.node.npm.task.NpxTask
import com.ibm.wala.gradle.forEachJavaProject
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion.VERSION_21

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
  id("com.ibm.wala.gradle.check-git-cleanliness")
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.javadoc")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("com.ibm.wala.gradle.project")
}

// to get the google-java-format jar and dependencies
repositories.mavenCentral()

JavaVersion.current().let {
  val minimumRequired = VERSION_21
  if (!it.isCompatibleWith(minimumRequired)) {
    throw GradleException(
        "Gradle is running on a Java $it JVM, which is not compatible with Java $minimumRequired. Build failures are likely. For advice on changing JVMs, visit <https://docs.gradle.org/current/userguide/build_environment.html> and look for discussion of the `org.gradle.java.home` Gradle property or the `JAVA_HOME` environment variable."
    )
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

val aggregatedJavadocClasspathExtras =
    configurations.register("aggregatedJavadocClasspathExtras") { isCanBeConsumed = false }

val aggregatedJavadocRuntimeElements =
    configurations.register("aggregatedJavadocRuntimeElements") { isCanBeConsumed = false }

val aggregatedJavadocSource =
    configurations.register("aggregatedJavadocSource") { isCanBeConsumed = false }

dependencies {
  // Some `compileOnly` dependencies are needed during Javadoc generation but are not included in
  // `aggregatedJavadocRuntimeElements`.
  aggregatedJavadocClasspathExtras(libs.jetbrains.annotations)
  aggregatedJavadocClasspathExtras(libs.nullaway.annotations)

  forEachJavaProject {
    aggregatedJavadocRuntimeElements(project(it.path, "runtimeElements"))
    aggregatedJavadocSource(project(it.path, "mainSourceElements"))
  }
}

tasks.register<Javadoc>("aggregatedJavadocs") {
  description = "Generate javadocs from all child projects as if they were a single project"
  group = "Documentation"
  destinationDir = layout.buildDirectory.dir("docs/javadoc").get().asFile
  title = "${project.name} $version API"
  (options as StandardJavadocDocletOptions).author(true)
  classpath = files(aggregatedJavadocClasspathExtras, aggregatedJavadocRuntimeElements)
  source(aggregatedJavadocSource)
  include("**/*.java")
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
        exclude("/.gradle/")
        exclude("**/build")
        include("**/*.sh")
      }
}

// Markdown

node {
  download = true
  version = "24.14.0"
}

val lintMarkdown =
    tasks.register<NpxTask>("lintMarkdown") {
      description = "Lint Markdown files"
      group = "verification"
      command = "markdownlint-cli2@0.18.1"
      val markdownFiles = fileTree(".") { include("*.md") }
      inputs.files(markdownFiles)
      inputs.file(".markdownlint-cli2.yaml")
      args = markdownFiles.map { it.path }
      outputs.run {
        file(layout.buildDirectory.file("$name.stamp"))
        cacheIf { true }
      }
      doLast { outputs.files.singleFile.createNewFile() }
    }

tasks.named("check") { dependsOn("buildHealth", lintMarkdown) }

tasks.named("shellcheck") { group = "verification" }

// install Java reformatter as git pre-commit hook
tasks.register<Copy>("installGitHooks") {
  description = "Install Git `pre-commit` hook for code reformatting"
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
//  Check for updated dependencies
//

tasks.withType<DependencyUpdatesTask>().configureEach {
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
