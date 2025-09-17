import com.ibm.wala.gradle.adHocDownload
import com.ibm.wala.gradle.dropTopDirectory
import com.ibm.wala.gradle.useCurrentJavaHome
import com.ibm.wala.gradle.valueToString

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.operating-system")
  id("com.ibm.wala.gradle.publishing")
}

val coreTestJar by configurations.registering { isCanBeConsumed = false }

val extraTestResources by configurations.registering { isCanBeConsumed = false }

val sampleCupSources by configurations.registering { isCanBeConsumed = false }

val isWindows: Boolean by extra

val platformsVersion by extra("android-28")

val unpackAndroidSdkInstaller by
    tasks.registering(Sync::class) {
      from({ zipTree(downloadAndroidSdk.singleFile) })
      into(layout.buildDirectory.dir(name))
    }

val installAndroidSdk by
    tasks.registering(Exec::class) {
      inputs.files(unpackAndroidSdkInstaller)
      val sdkManager =
          unpackAndroidSdkInstaller
              .map { it.destinationDir.resolve("cmdline-tools/bin/sdkmanager") }
              .valueToString

      val destinationDir = layout.buildDirectory.dir(name).valueToString
      outputs.run {
        dir(destinationDir)
        cacheIf { true }
      }

      // Running the Android SDK manager requires that `$JAVA_HOME` be set.
      useCurrentJavaHome()

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
                "$yes | $sdkManager --sdk_root=$destinationDir platforms$semicolon$platformsVersion >$discard",
            )
          }
    }

eclipse { synchronizationTasks(installAndroidSdk) }

dependencies {
  api(libs.dexlib2)
  api(projects.core)
  api(projects.shrike)
  api(projects.util)

  compileOnly(libs.jetbrains.annotations)

  coreTestJar(project("path" to ":core", "configuration" to "testJarConfig"))
  extraTestResources(project("path" to ":core", "configuration" to "dalvikTestResources"))

  implementation(libs.slf4j.api)
  implementation(libs.guava)

  sampleCupSources(libs.java.cup.map { "$it:sources" })

  testImplementation(libs.android.tools)
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(projects.dalvik)
  testImplementation(testFixtures(projects.core))
  testImplementation(testFixtures(projects.util))

  // directory containing "android.jar", which various tests want to find as a resource
  testRuntimeOnly(
      files(installAndroidSdk.map { "${it.outputs.files.singleFile}/platforms/$platformsVersion" })
  )
}

val downloadDroidBench =
    adHocDownload(
        uri("https://github.com/secure-software-engineering/DroidBench/archive/refs/tags"),
        "DroidBench_2.0",
        "zip",
    )

val unpackDroidBench by
    tasks.registering(Sync::class) {
      from({ zipTree(downloadDroidBench.singleFile) }) { include("*/apk/**") }
      into(layout.buildDirectory.dir("DroidBench"))
      dropTopDirectory()
    }

val downloadAndroidSdk = run {
  val osName: String by extra
  val sdkOs =
      when {
        "Linux".toRegex().containsMatchIn(osName) -> "linux"
        "Mac OS X".toRegex().containsMatchIn(osName) -> "mac"
        "Windows.*".toRegex().containsMatchIn(osName) -> "win"
        else -> throw GradleException("unrecognized operating system name \"$osName\"")
      }
  adHocDownload(
      uri("https://dl.google.com/android/repository"),
      "commandlinetools-$sdkOs",
      "zip",
      "7583922_latest",
  )
}

val extractSampleCup by
    tasks.registering(Sync::class) {
      from({ zipTree(sampleCupSources.get().singleFile) })
      into(layout.buildDirectory.file(name))
      include("parser.cup")
      rename { "sample.cup" }
    }

val downloadSampleLex =
    adHocDownload(
        uri("https://www.cs.princeton.edu/~appel/modern/java/JLex/current"),
        "sample",
        "lex",
    )

tasks.named<Copy>("processTestResources") {
  dependsOn(coreTestJar)
  from(downloadSampleLex) { eachFile { name = "sample.lex" } }
  from(extractSampleCup)
  from(extraTestResources)
  from({ zipTree(coreTestJar.get().singleFile) })
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
      )
  )
}
