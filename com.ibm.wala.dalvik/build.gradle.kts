import com.ibm.wala.gradle.VerifiedDownload

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

val coreTestJar: Configuration by configurations.creating { isCanBeConsumed = false }

val extraTestResources: Configuration by configurations.creating { isCanBeConsumed = false }

val sampleCupSources: Configuration by configurations.creating { isCanBeConsumed = false }

val installAndroidSdk by
    tasks.registering(Sync::class) {
      from(downloadAndroidSdk.map { zipTree(it.dest) })
      into(layout.buildDirectory.dir(name))

      val platformsVersion by extra("android-28")

      doLast {
        exec {

          // Running the Android SDK manager requires that `$JAVA_HOME` be set.
          environment(
              "JAVA_HOME",
              javaToolchains.launcherFor(java.toolchain).get().metadata.installationPath)

          data class Details(
              val shell: String,
              val shellFlags: String,
              val yes: String,
              val semicolon: String,
              val discard: String,
          )
          (if (isWindows)
                  Details(
                      "PowerShell",
                      "-Command",
                      "echo y",
                      "`;",
                      "\$null",
                  )
              else
                  Details(
                      "sh",
                      "-ceu",
                      "yes 2>/dev/null",
                      "\\;",
                      "/dev/null",
                  ))
              .run {
                commandLine(
                    shell,
                    shellFlags,
                    "$yes | $destinationDir/cmdline-tools/bin/sdkmanager --sdk_root=$destinationDir platforms$semicolon$platformsVersion >$discard")
              }
        }
      }

      outputs.cacheIf { true }
    }

eclipse { synchronizationTasks(installAndroidSdk) }

dependencies {
  coreTestJar(project("path" to ":com.ibm.wala.core", "configuration" to "testJarConfig"))
  extraTestResources(
      project("path" to ":com.ibm.wala.core", "configuration" to "dalvikTestResources"))

  implementation(libs.slf4j.api)
  implementation(libs.dexlib2)
  implementation(libs.guava)
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))

  sampleCupSources(libs.java.cup.map { "$it:sources" })

  testImplementation(libs.android.tools)
  testImplementation(libs.junit)
  testImplementation(libs.dexlib2)
  testImplementation(project(":com.ibm.wala.core"))
  testImplementation(project(":com.ibm.wala.dalvik"))
  testImplementation(project(":com.ibm.wala.shrike"))
  testImplementation(project(":com.ibm.wala.util"))
  testImplementation(testFixtures(project(":com.ibm.wala.core")))

  // directory containing "android.jar", which various tests want to find as a resource
  testRuntimeOnly(
      files(
          installAndroidSdk.map {
            "${it.outputs.files.singleFile}/platforms/${it.extra["platformsVersion"]}"
          }))
}

val downloadDroidBench by
    tasks.registering(VerifiedDownload::class) {
      src("https://codeload.github.com/secure-software-engineering/DroidBench/zip/DroidBench_2.0")
      dest(project.layout.buildDirectory.file("DroidBench_2.0.zip"))
      checksum("16726a48329835140e14f18470a1b4a3")
    }

val unpackDroidBench by
    tasks.registering(Sync::class) {
      from(downloadDroidBench.map { zipTree(it.dest) }) {
        eachFile {
          relativePath = RelativePath(!isDirectory, *relativePath.segments.drop(1).toTypedArray())
        }
      }

      into(layout.buildDirectory.dir("DroidBench"))
      includeEmptyDirs = false
    }

val downloadAndroidSdk by
    tasks.registering(VerifiedDownload::class) {
      val osName: String by rootProject.extra
      data class Details(
          val sdkOs: String,
          val checksum: String,
      )
      (when {
            "Linux".toRegex().containsMatchIn(osName) ->
                Details("linux", "124f2d5115eee365df6cf3228ffbca6fc3911d16f8025bebd5b1c6e2fcfa7faf")
            "Mac OS X".toRegex().containsMatchIn(osName) ->
                Details("mac", "6929a1957f3e71008adfade0cebd08ebea9b9f506aa77f1849c7bdc3418df7cf")
            "Windows.*".toRegex().containsMatchIn(osName) ->
                Details("win", "f9e6f91743bcb1cc6905648ca751bc33975b0dd11b50d691c2085d025514278c")
            else -> throw GradleException("unrecognized operating system name \"$osName\"")
          })
          .run {
            val archive = "commandlinetools-$sdkOs-7583922_latest.zip"
            src("https://dl.google.com/android/repository/$archive")
            dest(project.layout.buildDirectory.file(archive))
            checksum(checksum)
            algorithm("SHA-256")
          }
    }

val isWindows: Boolean by rootProject.extra

val extractSampleCup by
    tasks.registering {
      inputs.files(sampleCupSources)
      outputs.file(layout.buildDirectory.file("$name/sample.cup"))

      doLast {
        copy {
          from(zipTree(inputs.files.singleFile))
          include("parser.cup")
          rename { outputs.files.singleFile.name }
          into(outputs.files.singleFile.parent)
        }
      }
    }

val downloadSampleLex by
    tasks.registering(VerifiedDownload::class) {
      src("https://www.cs.princeton.edu/~appel/modern/java/JLex/current/sample.lex")
      dest(layout.buildDirectory.file("$name/sample.lex"))
      checksum("ae887758b2657981d023a72a165da830")
    }

tasks.named<Copy>("processTestResources") {
  dependsOn(coreTestJar)
  from(downloadSampleLex)
  from(extractSampleCup)
  from(extraTestResources)
  from(zipTree(coreTestJar.singleFile))
}

if (isWindows) tasks.named<Test>("test") { exclude("**/droidbench/**") }
else sourceSets.test.configure { resources.srcDir(unpackDroidBench) }

tasks.named<Test>("test") {
  maxHeapSize = "800M"

  outputs.files(
      layout.buildDirectory.files(
          "parser.java",
          "report",
          "sym.java",
      ))
}
