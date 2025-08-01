package com.ibm.wala.gradle

plugins {
  `java-test-fixtures`
  signing
  id("com.vanniktech.maven.publish")
}

val isSnapshot = "SNAPSHOT" in version as String

val java: JavaPluginExtension by extensions

val testFixturesJavadoc by
    tasks.existing(Javadoc::class) { destinationDir = java.docsDir.get().dir(name).asFile }

val testFixturesJavadocJar by
    tasks.registering(Jar::class) {
      archiveClassifier = "test-fixtures-javadoc"
      from(testFixturesJavadoc.map { it.destinationDir!! })
    }

mavenPublishing {
  configureBasedOnAppliedPlugins()
  publishToMavenCentral()
  signAllPublications()

  pom {
    name = property("POM_NAME") as String
    description = "T. J. Watson Libraries for Analysis"
    inceptionYear = "2006"
    url = "https://github.com/wala/WALA"
    val pomUrl = url

    ciManagement {
      system = "GitHub Actions"
      url = "https://github.com/wala/WALA/actions"
    }

    developers {
      // Current WALA maintainers, alphabetical by ID
      mapOf(
              "juliandolby" to "Julian Dolby",
              "liblit" to "Ben Liblit",
              "msridhar" to "Manu Sridharan",
              "sjfink" to "Stephen Fink",
          )
          .forEach { entry ->
            developer {
              id = entry.key
              name = entry.value
              url = "https://github.com/{$entry.key}"
            }
          }
    }

    issueManagement {
      system = "GitHub"
      url = pomUrl.map { "$it/issues" }
    }

    licenses {
      license {
        name = "Eclipse Public License v2.0"
        url = pomUrl.map { "$it/blob/master/LICENSE" }
      }
    }

    mailingLists {
      listOf(
              "commits",
              "wala",
          )
          .forEach { topic ->
            mailingList {
              name = "wala-$topic"
              archive = "https://sourceforge.net/p/wala/mailman/wala-$topic"
              subscribe = "https://sourceforge.net/projects/wala/lists/wala-$topic"
              unsubscribe = "https://sourceforge.net/projects/wala/lists/wala-$topic/unsubscribe"
              post = "wala-$topic@lists.sourceforge.net"
            }
          }
    }

    scm {
      url = pomUrl
      connection = "scm:git:git://github.com/wala/WALA.git"
      developerConnection = "scm:git:ssh://git@github.com/wala/WALA.git"
    }
  }
}

publishing {
  repositories.maven {
    name = "fakeRemote"
    setUrl(rootDir.resolve("build/maven-fake-remote-repository"))
  }

  publications.named<MavenPublication>("maven") {
    groupId = "com.ibm.wala"
    group = "com.ibm.wala"
    artifactId = base.archivesName.get()

    val testFixturesCodeElementsNames =
        listOf("testFixturesApiElements", "testFixturesRuntimeElements")
    testFixturesCodeElementsNames.forEach(this::suppressPomMetadataWarningsFor)

    if (java.sourceSets["testFixtures"].allSource.isEmpty) {
      // Test-fixtures jars would be empty except for manifests, so skip them in the style of
      // <https://docs.gradle.org/current/userguide/java_testing.html#ex-disable-publishing-of-test-fixtures-variants>.
      val javaComponent = components["java"] as AdhocComponentWithVariants
      listOf("Api", "Runtime", "Sources").forEach {
        javaComponent.withVariantsFromConfiguration(configurations["testFixtures${it}Elements"]) {
          skip()
        }
      }
    } else {
      // Test-fixtures jar will have real contents, so add Javadoc.
      artifact(testFixturesJavadocJar)
    }
  }
}

signing {
  setRequired {
    // Signatures are a hard requirement if publishing a non-snapshot to Maven Central.
    !isSnapshot &&
        gradle.taskGraph.allTasks.any {
          it is PublishToMavenRepository && it.name.endsWith("ToMavenCentralRepository")
        }
  }
}
