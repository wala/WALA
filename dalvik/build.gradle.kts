import com.ibm.wala.gradle.VerifiedDownload
import java.net.URI

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

val coreTestJar by configurations.registering { isCanBeConsumed = false }

val extraTestResources by configurations.registering { isCanBeConsumed = false }

val sampleCupSources by configurations.registering { isCanBeConsumed = false }

val isWindows: Boolean by rootProject.extra

val platformsVersion by extra("android-28")

interface InstallAndroidSdkServices {
  @get:Inject val exec: ExecOperations
}

val installAndroidSdk by
    tasks.registering(Sync::class) {
      from(downloadAndroidSdk.map { zipTree(it.dest) })
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
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(projects.dalvik)
  testImplementation(testFixtures(projects.core))

  // directory containing "android.jar", which various tests want to find as a resource
  testRuntimeOnly(
      files(installAndroidSdk.map { "${it.outputs.files.singleFile}/platforms/$platformsVersion" }))
}

val downloadDroidBench by
    tasks.registering(VerifiedDownload::class) {
      src =
          URI(
              "https://codeload.github.com/secure-software-engineering/DroidBench/zip/DroidBench_2.0")
      dest = project.layout.buildDirectory.file("DroidBench_2.0.zip")
      checksum = "16726a48329835140e14f18470a1b4a3"
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
            src = URI("https://dl.google.com/android/repository/$archive")
            dest = project.layout.buildDirectory.file(archive)
            this@registering.checksum = checksum
            algorithm = "SHA-256"
          }
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

val downloadSampleLex by
    tasks.registering(VerifiedDownload::class) {
      src = URI("https://www.cs.princeton.edu/~appel/modern/java/JLex/current/sample.lex")
      dest = layout.buildDirectory.file("$name/sample.lex")
      checksum = "ae887758b2657981d023a72a165da830"
    }

tasks.named<Copy>("processTestResources") {
  dependsOn(coreTestJar)
  from(downloadSampleLex)
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
