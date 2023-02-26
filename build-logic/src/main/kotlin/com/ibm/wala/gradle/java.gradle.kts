package com.ibm.wala.gradle

// Build configuration for subprojects that include Java source code.

import net.ltgt.gradle.errorprone.errorprone
import org.gradle.plugins.ide.eclipse.model.EclipseModel

plugins {
  eclipse
  `java-library`
  `java-test-fixtures`
  `maven-publish`
  signing
  id("com.diffplug.spotless")
  id("com.ibm.wala.gradle.javadoc")
  id("com.ibm.wala.gradle.subproject")
  id("net.ltgt.errorprone")
}

repositories {
  mavenCentral()
  // to get r8
  maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
}

val sourceSets = the<SourceSetContainer>()

configurations {
  create("ecj") { isCanBeConsumed = false }
  named("javadocClasspath").get().extendsFrom(compileClasspath.get())
}

dependencies {
  "ecj"(rootProject.the<VersionCatalogsExtension>().named("libs").findLibrary("eclipse-ecj").get())
  "errorprone"(
      rootProject
          .the<VersionCatalogsExtension>()
          .named("libs")
          .findLibrary("errorprone-core")
          .get())
  "javadocSource"(sourceSets.main.get().allJava)
}

the<JavaPluginExtension>().toolchain.languageVersion.set(JavaLanguageVersion.of(11))

tasks.withType<JavaCompile>().configureEach {
  options.errorprone {
    // don't run warning-level checks by default as they add too much noise to build output
    // NOTE: until https://github.com/google/error-prone/pull/3462 makes it to a release,
    // we need to customize the level of at least one specific check to make this flag work
    disableAllWarnings.set(true)
    // warning-level checks upgraded to error, since we've fixed all the warnings
    error("UnnecessaryParentheses")
    error("UnusedVariable")
    error("JdkObsolete")
    // checks we do not intend to try to fix in the near-term:
    // Just too many of these; proper Javadoc would be a great long-term goal
    disable("MissingSummary")
    // WALA has many optimizations involving using == to check reference equality.  They
    // may be unnecessary on modern JITs, but fixing these issues requires subtle changes
    // that could introduce bugs
    disable("ReferenceEquality")
    // Example for running Error Prone's auto-patcher.  To run, uncomment and change the
    // check name to the one you want to patch
    //			errorproneArgs.addAll(
    //					"-XepPatchChecks:UnnecessaryParentheses",
    //					"-XepPatchLocation:IN_PLACE"
    //			)
  }
}

configurations {
  all {
    resolutionStrategy.dependencySubstitution {
      substitute(module("org.hamcrest:hamcrest-core"))
          .using(
              module(
                  rootProject
                      .the<VersionCatalogsExtension>()
                      .named("libs")
                      .findLibrary("hamcrest")
                      .get()
                      .get()
                      .toString()))
          .because(
              "junit depends on hamcrest-core, but all hamcrest-core classes have been incorporated into hamcrest")
    }
  }

  "implementation" {
    // See https://github.com/wala/WALA/issues/823.  This group was renamed to
    // net.java.dev.jna.  The com.sun.jna dependency is only pulled in from
    // com.ibm.wala.ide.* projects.  Since we only try to compile those projects from
    // Gradle, but not run them, excluding the group as a dependence is a reasonable
    // solution.
    exclude(group = "com.sun.jna")
  }
}

the<EclipseModel>().synchronizationTasks("processTestResources")

tasks.named<Test>("test") {
  include("**/*Test.class")
  include("**/*TestCase.class")
  include("**/*Tests.class")
  include("**/Test*.class")
  exclude("**/*AndroidLibs*.class")

  val trial = project.findProperty("trial")
  if (trial != null) {
    outputs.upToDateWhen { false }
    afterTest(
        KotlinClosure2<TestDescriptor, TestResult, Unit>({ descriptor, result ->
          File("${rootProject.buildDir}/time-trials.csv").let {
            if (!it.exists()) {
              it.appendText("trial,className,name,resultType,startTime,endTime\n")
            }
            it.appendText(
                "$trial,${descriptor.className},${descriptor.name},${result.resultType},${result.startTime},${result.endTime}\n")
          }
        }))
  } else {
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
  }
}

if (project.hasProperty("excludeSlowTests")) {
  dependencies { testImplementation(testFixtures(project(":com.ibm.wala.core"))) }
  tasks.named<Test>("test") { useJUnit { excludeCategories("com.ibm.wala.tests.util.SlowTests") } }
}

val ecjCompileTaskProviders =
    sourceSets.map { sourceSet -> JavaCompileUsingEcj.withSourceSet(project, sourceSet) }

project.tasks.named("check") { dependsOn(ecjCompileTaskProviders) }

tasks.withType<JavaCompile>().configureEach {
  options.run {
    encoding = "UTF-8"
    compilerArgs.add("-Werror")
  }
}

tasks.withType<JavaCompileUsingEcj>().configureEach {

  // Allow skipping all ECJ compilation tasks by setting a project property.
  onlyIf { !project.hasProperty("skipJavaUsingEcjTasks") }

  // ECJ warning / error levels are set via a configuration file, not this argument
  options.compilerArgs.remove("-Werror")
}

// Special hack for WALA as an included build.  Composite
// builds only build and use artifacts from the default
// configuration of included builds:
// <https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations>.
// This known limitation makes WALA test fixtures unavailable
// when WALA is included in a composite build.  As a
// workaround for composite projects that rely on those test
// fixtures, we extend the main sourceSet to include all
// test-fixture sources too.  This hack is only applied when
// WALA itself is an included build.
if (project.gradle.parent != null) {
  afterEvaluate {
    sourceSets["main"].java.srcDirs(sourceSets["testFixtures"].java.srcDirs)

    dependencies { "implementation"(configurations["testFixturesImplementation"].dependencies) }
  }
}

spotless.java {
  googleJavaFormat(
      rootProject
          .the<VersionCatalogsExtension>()
          .named("libs")
          .findVersion("google-java-format")
          .get()
          .toString())
}
