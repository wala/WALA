import com.ibm.wala.gradle.CompileKawaScheme
import com.ibm.wala.gradle.JavaCompileUsingEcj
import com.ibm.wala.gradle.VerifiedDownload
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry
import org.gradle.plugins.ide.eclipse.model.Classpath

plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse {
  project.natures("org.eclipse.pde.PluginNature")
  classpath.file.whenMerged {
    (this as Classpath).run {
      entries.forEach {
        if (it is AbstractClasspathEntry && it.path == "src/testSubjects/java") {
          it.entryAttributes["ignore_optional_problems"] = true
        }
      }
    }
  }
}

sourceSets.create("testSubjects")

val compileTestSubjectsJava by tasks.existing(JavaCompile::class)

val ecjCompileJavaTestSubjects: TaskProvider<JavaCompileUsingEcj> =
    JavaCompileUsingEcj.withSourceSet(project, sourceSets["testSubjects"])

ecjCompileJavaTestSubjects.configure {
  options.compilerArgs.add("-warn:none")
  listOf(
          "serial",
          "unchecked",
          "unusedLocal",
          "unusedParam",
          "unusedThrown",
      )
      .forEach { options.compilerArgs.add("-err:-$it") }
}

tasks.named("check") { dependsOn(ecjCompileJavaTestSubjects) }

compileTestSubjectsJava.configure {
  // No need to run Error Prone on our analysis test inputs
  options.errorprone.isEnabled.set(false)
}

dependencies {
  api(project(":com.ibm.wala.shrike")) {
    because("public class Entrypoint implements interface BytecodeConstraints")
  }
  api(project(":com.ibm.wala.util")) {
    because("public interface CallGraph extends interface NumberedGraph")
  }
  testFixturesImplementation(libs.ant)
  testFixturesImplementation(libs.junit)
  testImplementation(libs.hamcrest)
  testImplementation(libs.junit)
  testRuntimeOnly(sourceSets["testSubjects"].output.classesDirs)
  // add the testSubjects source files to enable SourceMapTest to pass
  testRuntimeOnly(files(sourceSets["testSubjects"].java.srcDirs))
}

dependencies { javadocClasspath(project(":com.ibm.wala.dalvik")) }

////////////////////////////////////////////////////////////////////////
//
//  download and extract kawa 3.0 "kawa.jar"
//

val downloadKawa by
    tasks.registering(VerifiedDownload::class) {
      val archive = "kawa-3.0.zip"
      src("https://ftp.gnu.org/pub/gnu/kawa/$archive")
      dest(project.layout.buildDirectory.file(archive))
      checksum("2713e6dfb939274ba3b1d36daea68436")
    }

val extractKawa by
    tasks.registering {
      inputs.files(downloadKawa)
      outputs.file(layout.buildDirectory.file("$name/kawa.jar"))

      doLast {
        copy {
          from(zipTree(inputs.files.singleFile)) {
            include("kawa-*/lib/${outputs.files.singleFile.name}")
            eachFile { relativePath = RelativePath.parse(!isDirectory, relativePath.lastName) }
          }
          into(outputs.files.singleFile.parent)
          includeEmptyDirs = false
        }
      }
    }

////////////////////////////////////////////////////////////////////////
//
//  download, unpack, and build kawa chess
//

val kawaChessCommitHash = "f1d2dcc707a1ef19dc159e2eaee5aecc8a41d7a8"

val downloadKawaChess by
    tasks.registering(VerifiedDownload::class) {
      src("https://github.com/ttu-fpclub/kawa-chess/archive/${kawaChessCommitHash}.zip")
      dest(project.layout.buildDirectory.file("kawa-chess.zip"))
      checksum("cf29613d2be5f476a475ee28b4df9d9e")
    }

val unpackKawaChess by
    tasks.registering {
      inputs.files(downloadKawaChess)
      outputs.dir(project.layout.buildDirectory.file("kawa-chess-$kawaChessCommitHash"))

      doLast {
        copy {
          from(zipTree(inputs.files.singleFile))
          into(outputs.files.singleFile.parent)
        }
      }
    }

val compileKawaSchemeChessMain by
    tasks.registering(CompileKawaScheme::class) {
      schemeFile.fileProvider(
          unpackKawaChess.map { file("${it.outputs.files.singleFile}/main.scm") })
    }

val buildChessJar by
    tasks.registering(Jar::class) {
      from(compileKawaSchemeChessMain)
      destinationDirectory.set(project.layout.buildDirectory.dir(name))
      archiveFileName.set("kawachess.jar")
      archiveVersion.set(null as String?)
    }

////////////////////////////////////////////////////////////////////////
//
//  build the kawa test jar
//

val compileKawaSchemeTest by
    tasks.registering(CompileKawaScheme::class) {
      schemeFile.set(layout.projectDirectory.file("kawasrc/test.scm"))
    }

val buildKawaTestJar by
    tasks.registering(Jar::class) {
      from(compileKawaSchemeTest)
      destinationDirectory.set(project.layout.buildDirectory.dir(name))
      archiveFileName.set("kawatest.jar")
      archiveVersion.set(null as String?)
    }

////////////////////////////////////////////////////////////////////////
//
//  download and extract "bcel-5.2.jar"
//

val downloadBcel by
    tasks.registering(VerifiedDownload::class) {
      val basename by extra("bcel-5.2")
      val archive = "${basename}.tar.gz"
      src("https://archive.apache.org/dist/jakarta/bcel/binaries/$archive")
      dest(project.layout.buildDirectory.file(archive))
      checksum("19bffd7f217b0eae415f1ef87af2f0bc")
      useETag(false)
    }

val extractBcel by
    tasks.registering {
      val basename = downloadBcel.map { it.extra["basename"] as String }
      val jarFile = basename.flatMap { layout.buildDirectory.file("$name/${it}.jar") }
      inputs.files(downloadBcel)
      outputs.file(jarFile)

      doLast {
        copy {
          from(tarTree(inputs.files.singleFile)) {
            val downloadBcelBasename = basename.get()
            include("$downloadBcelBasename/$downloadBcelBasename.jar")
            eachFile { relativePath = RelativePath.parse(!isDirectory, relativePath.lastName) }
          }
          into(jarFile.get().asFile.parent)
          includeEmptyDirs = false
        }
      }
    }

////////////////////////////////////////////////////////////////////////
//
//  download "java-cup-11a.jar"
//

val downloadJavaCup by
    tasks.registering(VerifiedDownload::class) {
      val archive = "java-cup-11a.jar"
      src("http://www2.cs.tum.edu/projects/cup/$archive")
      dest(layout.buildDirectory.file("$name/$archive"))
      checksum("2bda8c40abd0cbc295d3038643d6e4ec")
    }

////////////////////////////////////////////////////////////////////////
//
//  collect "JLex.jar"
//

val collectJLexFrom: Configuration by configurations.creating { isCanBeConsumed = false }

dependencies {
  collectJLexFrom(
      project(
          mapOf("path" to ":com.ibm.wala.cast.java.test.data", "configuration" to "testJarConfig")))
}

val collectJLex by
    tasks.registering(Jar::class) {
      inputs.files(collectJLexFrom)
      from(zipTree(collectJLexFrom.singleFile))
      include("JLex/")
      archiveFileName.set("JLex.jar")
      destinationDirectory.set(layout.buildDirectory.dir(name))
    }

////////////////////////////////////////////////////////////////////////
//
//  generate "hello_hash.jar"
//

val downloadOcamlJava by
    tasks.registering(VerifiedDownload::class) {
      val version = "2.0-alpha1"
      val basename by extra("ocamljava-$version")
      val archive = "$basename.tar.gz"
      src("http://www.ocamljava.org/downloads/download.php?version=$version-bin")
      dest(project.layout.buildDirectory.file(archive))
      checksum("45feec6e3889f5073a39c2c4c84878d1")
    }

val unpackOcamlJava by
    tasks.registering(Sync::class) {
      from(downloadOcamlJava.map { tarTree(it.dest) })
      into(project.layout.buildDirectory.dir(name))
    }

val prepareGenerateHelloHashJar by
    tasks.registering(Copy::class) {
      from("ocaml/hello_hash.ml")
      val outputDir = project.layout.buildDirectory.dir(name)
      into(outputDir)
      extra["copiedOcamlSource"] = file("$outputDir/${source.singleFile.name}")
    }

val generateHelloHashJar by
    tasks.registering(JavaExec::class) {
      val ocamlSource = prepareGenerateHelloHashJar.map { it.extra["copiedOcamlSource"] as String }
      inputs.file(ocamlSource)

      val jarTarget = layout.projectDirectory.file("ocaml/hello_hash.jar")
      outputs.file(jarTarget)
      outputs.cacheIf { true }

      val downloadOcamlJavaBasename = downloadOcamlJava.map { it.extra["basename"] as String }
      inputs.property("downloadOcamlJavaBasename", downloadOcamlJavaBasename)

      val ocamlJavaJar =
          unpackOcamlJava.map {
            file("${it.destinationDir}/${downloadOcamlJavaBasename.get()}/lib/ocamljava.jar")
          }
      inputs.file(ocamlJavaJar)
      classpath(ocamlJavaJar)

      mainClass.set("ocaml.compilers.ocamljavaMain")
      args("-o", jarTarget)
      argumentProviders.add { listOf(ocamlSource.get()) }
    }

////////////////////////////////////////////////////////////////////////
//
//  collect "com.ibm.wala.core.testdata_1.0.0.jar"
//

val collectTestData by
    tasks.registering(Jar::class) {
      archiveFileName.set("com.ibm.wala.core.testdata_1.0.0.jar")
      from(compileTestSubjectsJava)
      from("classes")
      includeEmptyDirs = false
      destinationDirectory.set(layout.buildDirectory.dir(name))
    }

val collectTestDataJar: Configuration by configurations.creating { isCanBeResolved = false }

artifacts.add(collectTestDataJar.name, collectTestData)

////////////////////////////////////////////////////////////////////////
//
//  collect "com.ibm.wala.core.testdata_1.0.0a.jar"
//

val collectTestDataA by
    tasks.registering(Jar::class) {
      archiveFileName.set("com.ibm.wala.core.testdata_1.0.0a.jar")
      from(compileTestSubjectsJava)
      from("classes")
      includeEmptyDirs = false
      destinationDirectory.set(layout.buildDirectory.dir(name))
      exclude(
          "**/CodeDeleted.class",
          "**/SortingExample.class",
          "**/A.class",
      )
    }

////////////////////////////////////////////////////////////////////////

tasks.named<Copy>("processTestResources") {
  from(
      buildChessJar,
      buildKawaTestJar,
      collectJLex,
      collectTestData,
      downloadJavaCup,
      extractBcel,
      extractKawa,
  )

  // If "ocaml/hello_hash.jar" exists, then treat it as up-to-date and ready to use.  But if it is
  // missing, then use the generateHelloHashJar task to rebuild it.  The latter will entail
  // downloading OCaml-Java if we haven"t already: something we prefer to avoid.
  val helloHashJar = generateHelloHashJar.get().outputs.files.singleFile
  from(if (helloHashJar.exists()) helloHashJar else generateHelloHashJar)
}

tasks.named<Test>("test") {
  maxHeapSize = "1500M"
  systemProperty("com.ibm.wala.junit.profile", "short")
  classpath += files(project(":com.ibm.wala.core").sourceSets.test.get().output.classesDirs)
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

val testResources: Configuration by configurations.creating { isCanBeResolved = false }

artifacts.add(testResources.name, sourceSets.test.map { it.resources.srcDirs.single() })

////////////////////////////////////////////////////////////////////////

val testJar by
    tasks.registering(Jar::class) {
      group = "build"
      archiveClassifier.set("test")
      from(tasks.named("compileTestJava"))
    }

val testJarConfig: Configuration by configurations.creating { isCanBeResolved = false }

artifacts.add(testJarConfig.name, testJar)

val dalvikTestResources: Configuration by configurations.creating { isCanBeResolved = false }

listOf(
        collectJLex,
        collectTestDataA,
        downloadJavaCup,
        extractBcel,
    )
    .forEach { artifacts.add(dalvikTestResources.name, it) }
