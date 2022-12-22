import com.ibm.wala.gradle.VerifiedDownload
import net.ltgt.gradle.errorprone.errorprone

plugins { id("com.ibm.wala.gradle.java") }

val compileTestJava by
    tasks.existing(JavaCompile::class) {
      options.run {
        // No need to run Error Prone on our analysis test inputs
        errorprone.isEnabled.set(false)
        // Some code in the test data is written in a deliberately bad style, so allow warnings
        compilerArgs.remove("-Werror")
        compilerArgs.add("-nowarn")
      }
    }

val testJar by
    tasks.registering(Jar::class) {
      group = "build"
      archiveClassifier.set("test")
      from(compileTestJava)
    }

val testJarConfig: Configuration by configurations.creating { isCanBeResolved = false }

val testJavaSourceDirectory: Configuration by configurations.creating { isCanBeResolved = false }

artifacts {
  add(testJarConfig.name, testJar)
  add(testJavaSourceDirectory.name, sourceSets.test.map { it.java.srcDirs.first() })
}

////////////////////////////////////////////////////////////////////////
//
//  download JLex
//

val downloadJLex by
    tasks.registering(VerifiedDownload::class) {
      src("https://www.cs.princeton.edu/~appel/modern/java/JLex/current/Main.java")
      checksum("fe0cff5db3e2f0f5d67a153cf6c783af")
      val downloadedSourceDir by extra(layout.buildDirectory.dir(name))
      dest(downloadedSourceDir.map { "$it/JLex/Main.java" })
    }

sourceSets.test.get().java.srcDir(downloadJLex.map { it.extra["downloadedSourceDir"]!! })

////////////////////////////////////////////////////////////////////////
//
//  create Eclipse metadata for use by Maven when running
//  com.ibm.wala.cast.java.test.JDTJavaIRTests and
//  com.ibm.wala.cast.java.test.JDTJava15IRTests tests
//

tasks.register("prepareMavenBuild") { dependsOn("eclipseClasspath", "eclipseProject") }
