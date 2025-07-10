import com.ibm.wala.gradle.adHocDownload

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

interface InstallAndroidSdkServices {
  @get:Inject val exec: ExecOperations
}

val installAndroidSdk by
    tasks.registering(Sync::class) {
      from(zipTree { downloadAndroidSdk.singleFile })
      into(layout.buildDirectory.dir(name))

      // When the task is actually executing (i.e.,in the `doLast` code below), the Gradle
      // configuration cache forbids us from accessing the current project. Instead, we use the
      // current project here, at task *configuration* time, to grab some values that we will later
      // use at task *execution* time.
      val isWindows = isWindows
      val javaLauncher = javaToolchains.launcherFor(java.toolchain)
      val platformsVersion = platformsVersion

      objects.newInstance<InstallAndroidSdkServices>().run {
        doLast {
          exec.exec {

            // Running the Android SDK manager requires that `$JAVA_HOME` be set.
            environment("JAVA_HOME", javaLauncher.get().metadata.installationPath)

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
      }

      outputs.cacheIf { true }
    }

eclipse { synchronizationTasks(installAndroidSdk) }

dependencies {
  api(libs.dexlib2)
  api(projects.core)
  api(projects.shrike)
  api(projects.util)

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
      files(installAndroidSdk.map { "${it.outputs.files.singleFile}/platforms/$platformsVersion" }))
}

val downloadDroidBench =
    adHocDownload(
        uri("https://github.com/secure-software-engineering/DroidBench/archive/refs/tags"),
        "DroidBench_2.0",
        "zip")

val unpackDroidBench by
    tasks.registering(Sync::class) {
      from(zipTree { downloadDroidBench.singleFile }) {
        eachFile {
          relativePath = RelativePath(!isDirectory, *relativePath.segments.drop(1).toTypedArray())
        }
      }

      into(layout.buildDirectory.dir("DroidBench"))
      includeEmptyDirs = false
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
      "7583922_latest")
}

interface ExtractSampleCupServices {
  @get:Inject val archive: ArchiveOperations
  @get:Inject val fileSystem: FileSystemOperations
}

val extractSampleCup by
    tasks.registering {
      inputs.files(sampleCupSources)
      outputs.file(layout.buildDirectory.file("$name/sample.cup"))

      objects.newInstance<ExtractSampleCupServices>().run {
        doLast {
          fileSystem.copy {
            from(archive.zipTree(inputs.files.singleFile))
            include("parser.cup")
            rename { outputs.files.singleFile.name }
            into(outputs.files.singleFile.parent)
          }
        }
      }
    }

val downloadSampleLex =
    adHocDownload(
        uri("https://www.cs.princeton.edu/~appel/modern/java/JLex/current"), "sample", "lex")

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
      ))
}
