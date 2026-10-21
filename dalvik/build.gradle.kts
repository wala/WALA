import com.ibm.wala.gradle.InstallAndroidSdk
import com.ibm.wala.gradle.adHocDownload
import com.ibm.wala.gradle.dropTopDirectory
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EMBEDDED
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.Category.VERIFICATION
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.operating-system")
  id("com.ibm.wala.gradle.publishing")
}

walaPublishing.pomName = "WALA Dalvik"

val coreTestJar =
    configurations.register("coreTestJar") {
      isCanBeConsumed = false
      attributes {
        attribute(BUNDLING_ATTRIBUTE, named(EXTERNAL))
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val extraTestResources =
    configurations.register("extraTestResources") {
      isCanBeConsumed = false
      attributes {
        attribute(BUNDLING_ATTRIBUTE, named(EMBEDDED))
        attribute(CATEGORY_ATTRIBUTE, named(VERIFICATION))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val sampleCupSources =
    configurations.register("sampleCupSources") {
      isCanBeConsumed = false
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

val isWindows = extra["isWindows"] as Boolean

@Suppress("RemoveUnnecessaryParentheses")
val catalogPlatformsVersion = libs.versions.android.platforms

val unpackAndroidSdkInstaller =
    tasks.register<Sync>("unpackAndroidSdkInstaller") {
      description = "Unpack Android SDK command-line tools"
      from(zipTree(downloadAndroidSdk))
      into(layout.buildDirectory.dir(name))
    }

val installAndroidSdk =
    tasks.register<InstallAndroidSdk>("installAndroidSdk") {
      sdkCommandLineTools = unpackAndroidSdkInstaller.flatMap { it.destinationDirectory }
      platformsVersion = catalogPlatformsVersion
    }

eclipse { synchronizationTasks(installAndroidSdk) }

repositories.maven("https://storage.googleapis.com/r8-releases/raw").content {
  includeGroup("com.android.tools")
}

dependencies {
  api(libs.dexlib2)
  api(libs.jspecify)
  api(projects.core)
  api(projects.shrike)
  api(projects.util)

  compileOnly(libs.jetbrains.annotations)

  coreTestJar(project(":core"))
  extraTestResources(project(":core"))

  implementation(libs.slf4j.api)
  implementation(libs.guava)

  sampleCupSources(libs.javaCup.map { "$it:sources" })

  testImplementation(libs.r8)
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(projects.dalvik)
  testImplementation(testFixtures(projects.core))
  testImplementation(testFixtures(projects.util))
}

val downloadDroidBench =
    adHocDownload(
        uri("https://github.com/secure-software-engineering/DroidBench/archive/refs/tags"),
        "DroidBench_2.0",
        "zip",
    )

val unpackDroidBench =
    tasks.register<Sync>("unpackDroidBench") {
      description = "Unpack DroidBench benchmark APKs"
      from(zipTree(downloadDroidBench)) { include("*/apk/**") }
      into(layout.buildDirectory.dir("DroidBench"))
      dropTopDirectory()
    }

val downloadAndroidSdk = run {
  val osName = extra["osName"] as String
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

val extractSampleCup =
    tasks.register<Sync>("extractSampleCup") {
      description = "Extract `sample.cup` grammar file"
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
  from(
      installAndroidSdk.zip(catalogPlatformsVersion) { sdk, version ->
        "${sdk.outputs.files.singleFile}/platforms/$version/android.jar"
      }
  )
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
