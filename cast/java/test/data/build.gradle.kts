import com.ibm.wala.gradle.adHocDownload
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.DOCUMENTATION
import org.gradle.api.attributes.Category.VERIFICATION
import org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import org.gradle.api.attributes.DocsType.SOURCES
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.test-subjects")
}

val compileTestSubjectsJava =
    tasks.named<JavaCompile>("compileTestSubjectsJava") {
      options.run {
        // No need to run Error Prone on our analysis test inputs
        errorprone.enabled = false
        // Some code in the test data is written in a deliberately bad style, so allow warnings
        compilerArgs.remove("-Werror")
        compilerArgs.add("-nowarn")
        isDeprecation = false
      }
    }

val testJar =
    tasks.register<Jar>("testJar") {
      description = "Assemble test JAR archive"
      group = "build"
      archiveClassifier = "test"
      from(compileTestSubjectsJava)
    }

val testJarConfig =
    configurations.register("testJarConfig") {
      isCanBeResolved = false
      attributes {
        attribute(BUNDLING_ATTRIBUTE, named(EXTERNAL))
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val testJavaSourceDirectory =
    configurations.register("testJavaSourceDirectory") {
      isCanBeResolved = false
      // Java sources as analysis inputs, typed per Gradle's own sourcesElements convention.
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(DOCUMENTATION))
        attribute(DOCS_TYPE_ATTRIBUTE, named(SOURCES))
      }
    }

val testSubjects = sourceSets.named("testSubjects")

artifacts {
  add(testJarConfig.name, testJar)
  add(testJavaSourceDirectory.name, testSubjects.map { it.java.srcDirs.first() })
}

// Exclude since various tests make assertions based on
// source positions in the test inputs.  To auto-format
// we also need to update the test assertions
spotless { java { target(files()) } }

////////////////////////////////////////////////////////////////////////
//
//  download JLex
//

val jLex =
    adHocDownload(
        uri("https://www.cs.princeton.edu/~appel/modern/java/JLex/current"),
        "Main",
        "java",
    )

val downloadJLex =
    tasks.register<Sync>("downloadJLex") {
      description = "Download JLex `Main.java` source"
      from(jLex) { eachFile { name = "Main.java" } }
      into(layout.buildDirectory.dir(name))
    }

testSubjects { java.srcDir(downloadJLex) }

////////////////////////////////////////////////////////////////////////
//
//  create Eclipse metadata for use by Maven when running
//  com.ibm.wala.cast.java.test.JDTJavaIRTests and
//  com.ibm.wala.cast.java.test.JDTJava15IRTests tests
//

tasks.register("prepareMavenBuild") {
  description = "Prepare Eclipse project metadata for Maven-based tests"
  dependsOn("eclipseClasspath", "eclipseProject")
}
