import com.ibm.wala.gradle.CompileKawaScheme
import com.ibm.wala.gradle.JavaCompileUsingEcj
import com.ibm.wala.gradle.adHocDownload
import com.ibm.wala.gradle.dropTopDirectory
import com.ibm.wala.gradle.valueToString
import kotlin.io.resolve
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.Classpath

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.operating-system")
  id("com.ibm.wala.gradle.publishing")
  id("com.ibm.wala.gradle.test-subjects")
}

eclipse {
  project.natures("org.eclipse.pde.PluginNature")
  classpath.file.whenMerged {
    this as Classpath
    entries.forEach {
      if (it is AbstractClasspathEntry && it.path == "src/testSubjects/java") {
        it.entryAttributes["ignore_optional_problems"] = true
      }
    }
  }
}

val compileTestSubjectsJava by tasks.existing

tasks {
  named<JavaCompile>("compileTestSubjectsJava") { options.isDeprecation = false }

  named<JavaCompileUsingEcj>("compileTestSubjectsJavaUsingEcj") {
    options.compilerArgumentProviders.add {
      listOf(
          "-warn:none",
          "-err:-serial",
          "-err:-unchecked",
          "-err:-unusedLocal",
          "-err:-unusedParam",
          "-err:-unusedThrown",
      )
    }
  }
}

dependencies {
  api(projects.shrike) {
    because("public class Entrypoint implements interface BytecodeConstraints")
  }
  api(projects.util) { because("public interface CallGraph extends interface NumberedGraph") }
  api(libs.jspecify)
  compileOnly(libs.jetbrains.annotations)
  testCompileOnly(libs.jetbrains.annotations)
  testFixturesApi(libs.assertj.core)
  testFixturesApi(libs.junit.jupiter.api)
  testFixturesApi(projects.shrike)
  testFixturesImplementation(libs.ant)
  testFixturesImplementation(libs.junit.platform.engine)
  testFixturesImplementation(libs.junit.platform.launcher)
  testFixturesImplementation(projects.util)
  implementation(libs.gson)
  testImplementation(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(testFixtures(projects.util))
  testRuntimeOnly(sourceSets["testSubjects"].output.classesDirs)
  // add the testSubjects source files to enable SourceMapTest to pass
  testRuntimeOnly(files(sourceSets["testSubjects"].java.srcDirs))
  // to allow writing test subject classes that use JUnit annotations
  "testSubjectsImplementation"(libs.junit.jupiter.api)
  "testSubjectsImplementation"(platform(libs.junit.bom))
}

// Injected services used by several tasks that extract selected files from downloads.
interface ExtractServices {
  @get:Inject val archive: ArchiveOperations
  @get:Inject val fileSystem: FileSystemOperations
}

////////////////////////////////////////////////////////////////////////
//
//  download and extract kawa 3.0 "kawa.jar"
//

val kawa = adHocDownload(uri("https://ftpmirror.gnu.org/gnu/kawa"), "kawa", "zip", "3.0")

val extractKawa by
    tasks.registering {
      inputs.files(kawa)
      outputs.file(layout.buildDirectory.file("$name/kawa.jar"))

      objects.newInstance<ExtractServices>().run {
        doLast {
          fileSystem.copy {
            from(archive.zipTree(inputs.files.singleFile)) {
              include("kawa-*/lib/${outputs.files.singleFile.name}")
              eachFile { relativePath = RelativePath.parse(!isDirectory, relativePath.lastName) }
            }
            into(outputs.files.singleFile.parent)
            includeEmptyDirs = false
          }
        }
      }
    }

////////////////////////////////////////////////////////////////////////
//
//  download, unpack, and build kawa chess
//

val kawaChessCommitHash = "f1d2dcc707a1ef19dc159e2eaee5aecc8a41d7a8"

val kawaChess =
    adHocDownload(
        uri("https://github.com/ttu-fpclub/kawa-chess/archive"),
        kawaChessCommitHash,
        "zip",
    )

val unpackKawaChess by
    tasks.registering(Sync::class) {
      from({ zipTree(kawaChess.singleFile) })
      into(layout.buildDirectory.dir(name))
      dropTopDirectory()
    }

val compileKawaSchemeChessMain by
    tasks.registering(CompileKawaScheme::class) {
      schemeFile = unpackKawaChess.map { it.destinationDir.resolve("main.scm") }
    }

val buildChessJar by
    tasks.registering(Jar::class) {
      from(compileKawaSchemeChessMain)
      destinationDirectory = layout.buildDirectory.dir(name)
      archiveFileName = "kawachess.jar"
      archiveVersion = null as String?
    }

////////////////////////////////////////////////////////////////////////
//
//  build the kawa test jar
//

val compileKawaSchemeTest by
    tasks.registering(CompileKawaScheme::class) {
      schemeFile = layout.projectDirectory.file("kawasrc/test.scm")
    }

val buildKawaTestJar by
    tasks.registering(Jar::class) {
      from(compileKawaSchemeTest)
      destinationDirectory = layout.buildDirectory.dir(name)
      archiveFileName = "kawatest.jar"
      archiveVersion = null as String?
    }

////////////////////////////////////////////////////////////////////////
//
//  download and extract "bcel-5.2.jar"
//

val downloadBcel =
    adHocDownload(
        uri("https://archive.apache.org/dist/jakarta/bcel/binaries"),
        "bcel",
        "tar.gz",
        "5.2",
    )

val extractBcel by
    tasks.registering(Sync::class) {
      from({ tarTree(downloadBcel.singleFile) })
      include("**/*.jar")
      into(layout.buildDirectory.map { "$it/$name" })
      eachFile { relativePath = RelativePath.parse(!isDirectory, relativePath.lastName) }
      includeEmptyDirs = false
    }

////////////////////////////////////////////////////////////////////////
//
//  download "java-cup-11a.jar"
//

val downloadJavaCup =
    adHocDownload(uri("https://www2.cs.tum.edu/projects/cup"), "java-cup", "jar", "11a")

val copyJavaCup by
    tasks.registering(Sync::class) {
      from(downloadJavaCup)
      into(layout.buildDirectory.dir(name))
    }

////////////////////////////////////////////////////////////////////////
//
//  collect "JLex.jar"
//

val collectJLexFrom by configurations.registering { isCanBeConsumed = false }

dependencies {
  collectJLexFrom(
      project(mapOf("path" to ":cast:java:test:data", "configuration" to "testJarConfig"))
  )
}

val collectJLex by
    tasks.registering(Jar::class) {
      inputs.files(collectJLexFrom)
      from({ zipTree(collectJLexFrom.get().singleFile) })
      include("JLex/")
      archiveFileName = "JLex.jar"
      destinationDirectory = layout.buildDirectory.dir(name)
    }

////////////////////////////////////////////////////////////////////////
//
//  generate "hello_hash.jar"
//

val ocamlJavaVersion = "2.0-alpha3"

val downloadOcamlJava =
    adHocDownload(
        uri("http://www.ocamljava.org/files/distrib"),
        "ocamljava",
        "tar.gz",
        ocamlJavaVersion,
        "bin",
    )

// Ideally this would be a `Sync` task using `from({ tarTree(downloadOcamlJava.singleFile) })`.
// However, this specific tar archive contains a member with a leading slash, and that apparently
// causes Gradle's native tar support to fail.
val unpackOcamlJava by
    tasks.registering(Exec::class) {
      inputs.files(downloadOcamlJava)
      executable = "tar"
      argumentProviders.add {
        listOf(
            "xzf",
            inputs.files.singleFile.path,
        )
      }
      val outputDir = layout.buildDirectory.dir(name)
      workingDir(outputDir)
      outputs.dir(outputDir)
    }

val generateHelloHashJar by
    tasks.registering(Exec::class) {

      // haven't been able to get `ocamljava.bat` to work on Windows yet
      val isWindows = project.extra["isWindows"] as Boolean
      onlyIf { !isWindows }

      // OCaml source file to compile to Java bytecode
      val ocamlSource = file("ocaml/hello_hash.ml")
      inputs.file(ocamlSource)

      // generated JAR archive that will contain compiled OCaml code
      val jarTarget = layout.buildDirectory.file("hello_hash.jar")
      outputs.file(jarTarget)
      outputs.cacheIf { true }
      argumentProviders.add { listOf("-o", jarTarget.get().toString()) }

      // unpacked OCaml-Java distribution
      inputs.files(unpackOcamlJava)

      // `ocamljava` script to compile OCaml to Java bytecode
      executable(
          unpackOcamlJava
              .map {
                it.workingDir.resolve(
                    "ocamljava-$ocamlJavaVersion/bin/ocamljava${if (isWindows) ".bat" else ""}"
                )
              }
              .valueToString
      )

      // use same JVM that is being used to run Gradle itself
      environment("JAVA_HOME", providers.systemProperty("java.home").valueToString)

      objects.newInstance<ExtractServices>().run {

        // avoid polluting source directory with `hello_hash.{cmi,cmj,jo}` files
        doFirst {
          fileSystem.copy {
            from(ocamlSource)
            into(temporaryDir)
          }
        }
        argumentProviders.add { listOf(temporaryDir.resolve(ocamlSource.name).toString()) }

        // validate generated JAR: `ocamljava` always "succeeds", even if something went wrong
        doLast { check(!archive.zipTree(jarTarget).isEmpty) }
      }
    }

////////////////////////////////////////////////////////////////////////
//
//  collect "com.ibm.wala.core.testdata_1.0.0.jar"
//

val collectTestData by
    tasks.registering(Jar::class) {
      archiveFileName = "com.ibm.wala.core.testdata_1.0.0.jar"
      from(compileTestSubjectsJava)
      from("classes")
      includeEmptyDirs = false
      destinationDirectory = layout.buildDirectory.dir(name)
    }

val collectTestDataJar by configurations.registering { isCanBeResolved = false }

artifacts.add(collectTestDataJar.name, collectTestData.map { it.destinationDirectory })

////////////////////////////////////////////////////////////////////////
//
//  collect "com.ibm.wala.core.testdata_1.0.0a.jar" for Dalvik tests
//

val collectTestDataAForDalvik by
    tasks.registering(Jar::class) {
      archiveFileName = "com.ibm.wala.core.testdata_1.0.0a.jar"
      from(compileTestSubjectsJava)
      from("classes")
      includeEmptyDirs = false
      destinationDirectory = layout.buildDirectory.dir(name)
      exclude(
          // This is an invalid class so don't include it; it causes D8 to crash
          "**/CodeDeleted.class",
      )
    }

////////////////////////////////////////////////////////////////////////

tasks.named<Copy>("processTestResources") {
  from(
      buildChessJar,
      buildKawaTestJar,
      collectJLex,
      collectTestData,
      copyJavaCup,
      extractBcel,
      extractKawa,
      generateHelloHashJar,
  )
}

tasks.named<Test>("test") {
  maxHeapSize = "2000M"
  systemProperty("com.ibm.wala.junit.profile", "short")
  classpath += files(sourceSets.test.get().output.classesDirs)
  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    events("passed", "skipped", "failed")
  }
  // temporarily turn off some tests on JDK 11+
  if (JavaVersion.current() >= JavaVersion.VERSION_11) {
    exclude("**/cha/LibraryVersionTest.class") // https://github.com/wala/WALA/issues/963
  }

  outputs.file(layout.buildDirectory.file("report"))
}

val testResources by configurations.registering { isCanBeResolved = false }

artifacts.add(testResources.name, sourceSets.test.map { it.resources.srcDirs.single() })

////////////////////////////////////////////////////////////////////////

val testJar by
    tasks.registering(Jar::class) {
      group = "build"
      archiveClassifier = "test"
      from(tasks.named("compileTestJava"))
    }

val testJarConfig by configurations.registering { isCanBeResolved = false }

artifacts.add(testJarConfig.name, testJar)

val dalvikTestResources by configurations.registering { isCanBeResolved = false }

listOf(
        collectJLex,
        collectTestDataAForDalvik,
        copyJavaCup,
        extractBcel,
    )
    .forEach { artifacts.add(dalvikTestResources.name, it) }
