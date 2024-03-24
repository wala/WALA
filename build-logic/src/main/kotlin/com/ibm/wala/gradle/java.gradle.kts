package com.ibm.wala.gradle

// Build configuration for subprojects that include Java source code.

import net.ltgt.gradle.errorprone.errorprone

plugins {
  eclipse
  `java-library`
  `java-test-fixtures`
  `maven-publish`
  signing
  id("com.diffplug.spotless")
  id("com.ibm.wala.gradle.eclipse-compatible-java")
  id("com.ibm.wala.gradle.javadoc")
  id("com.ibm.wala.gradle.subproject")
  id("net.ltgt.errorprone")
}

repositories {
  mavenCentral()
  // to get r8
  maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
}

java.toolchain.languageVersion =
    JavaLanguageVersion.of(property("com.ibm.wala.jdk-version") as String)

base.archivesName = "com.ibm.wala${path.replace(':', '.')}"

configurations {
  resolvable("ecj")
  named("javadocClasspath") { extendsFrom(compileClasspath.get()) }
}

fun findLibrary(alias: String) = rootProject.versionCatalogs.named("libs").findLibrary(alias).get()

dependencies {
  "ecj"(findLibrary("eclipse-ecj"))
  "errorprone"(findLibrary("errorprone-core"))
  "javadocSource"(sourceSets.main.get().allJava)

  testFixturesImplementation(platform(findLibrary("junit-bom")))
  testFixturesImplementation(findLibrary("junit-jupiter-api"))

  testImplementation(platform(findLibrary("junit-bom")))
  testImplementation(findLibrary("junit-jupiter-api"))
  testRuntimeOnly(findLibrary("junit-jupiter-engine"))
  testRuntimeOnly(findLibrary("junit-vintage-engine"))
}

tasks.withType<JavaCompile>().configureEach {
  // Generate JDK 11 bytecodes; that is the minimum version supported by WALA
  options.release = 11
  options.errorprone {
    // don't run warning-level checks by default as they add too much noise to build output
    // NOTE: until https://github.com/google/error-prone/pull/3462 makes it to a release,
    // we need to customize the level of at least one specific check to make this flag work
    disableAllWarnings = true
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
    //			errorproneArgs.appendAll(
    //					"-XepPatchChecks:UnnecessaryParentheses",
    //					"-XepPatchLocation:IN_PLACE"
    //			)
  }
}

eclipse.synchronizationTasks("processTestResources")

tasks.named<Test>("test") {
  useJUnitPlatform()

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
          rootProject.layout.buildDirectory.file("time-trials.csv").get().asFile.let {
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

if (hasProperty("excludeSlowTests")) {
  dependencies { testImplementation(testFixtures(project(":core"))) }
  tasks.named<Test>("test") { useJUnitPlatform { excludeTags("slow") } }
}

val ecjCompileTaskProviders =
    sourceSets.map { sourceSet -> JavaCompileUsingEcj.withSourceSet(project, sourceSet) }

tasks.named("check") { dependsOn(ecjCompileTaskProviders) }

tasks.withType<JavaCompile>().configureEach {
  options.run {
    encoding = "UTF-8"
    compilerArgs.add("-Werror")
  }
}

tasks.withType<JavaCompileUsingEcj>().configureEach {

  // Allow skipping all ECJ compilation tasks by setting a project property.
  val skipJavaUsingEcjTasks = project.hasProperty("skipJavaUsingEcjTasks")
  onlyIf { !skipJavaUsingEcjTasks }

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
if (gradle.parent != null) {
  afterEvaluate {
    sourceSets["main"].java.srcDirs(sourceSets["testFixtures"].java.srcDirs)

    dependencies { "implementation"(configurations["testFixturesImplementation"].dependencies) }
  }
}

spotless {
  java {
    googleJavaFormat(
        rootProject.versionCatalogs
            .named("libs")
            .findVersion("google-java-format")
            .get()
            .toString())
  }
}
