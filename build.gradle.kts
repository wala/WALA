////////////////////////////////////////////////////////////////////////
//
//  plugin configuration must precede everything else
//

import com.autonomousapps.tasks.BuildHealthTask
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.ibm.wala.gradle.LintMarkdown
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion.VERSION_21
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME

plugins {
  idea
  alias(libs.plugins.dependency.analysis)
  alias(libs.plugins.file.lister)
  alias(libs.plugins.node)
  alias(libs.plugins.shellcheck)
  alias(libs.plugins.version.catalog.update)
  alias(libs.plugins.versions)
  id("com.ibm.wala.gradle.check-git-cleanliness")
  id("com.ibm.wala.gradle.project")
}

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

val lintMarkdown = tasks.register<LintMarkdown>("lintMarkdown")

tasks.named(CHECK_TASK_NAME) { dependsOn(tasks.withType<BuildHealthTask>(), lintMarkdown) }

tasks.named("shellcheck") { group = "verification" }

// install Java reformatter as git pre-commit hook
tasks.register<Copy>("installGitHooks") {
  description = "Install Git `pre-commit` hook for code reformatting"
  from("config/hooks/pre-commit-stub")
  rename { "pre-commit" }
  destinationDirectory = layout.projectDirectory.dir(".git/hooks")
  filePermissions {
    listOf(user, group, other).forEach {
      it.read = true
      it.write = true
      it.execute = true
    }
  }
}

listOf(CHECK_TASK_NAME, "spotlessCheck", "spotlessApply").forEach {
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
