package com.ibm.wala.gradle

plugins {
  `java-library`
  `java-test-fixtures`
  `maven-publish`
  signing
  id("com.ibm.wala.gradle.subproject")
}

val isSnapshot = "SNAPSHOT" in version as String

val javaComponent = components["java"] as AdhocComponentWithVariants

val allTestFixturesSource: SourceDirectorySet = sourceSets.testFixtures.get().allSource

val testFixturesJavadoc by
    tasks.existing(Javadoc::class) { setDestinationDir(java.docsDir.get().dir(name).asFile) }

val testFixturesJavadocJar by
    tasks.registering(Jar::class) {
      archiveClassifier = "test-fixtures-javadoc"
      from(testFixturesJavadoc.map { it.destinationDir!! })
    }

val testFixturesSourcesJar by
    tasks.registering(Jar::class) {
      archiveClassifier = "test-fixtures-sources"
      from(allTestFixturesSource)
    }

val publishing: PublishingExtension by extensions

val mavenPublication =
    publishing.publications.create<MavenPublication>("maven") {
      from(javaComponent)

      groupId = "com.ibm.wala"
      artifactId = base.archivesName.get()

      val testFixturesCodeElementsNames =
          listOf("testFixturesApiElements", "testFixturesRuntimeElements")
      testFixturesCodeElementsNames.forEach(this::suppressPomMetadataWarningsFor)

      if (allTestFixturesSource.isEmpty) {
        // Test-fixtures jar would be empty except for the manifest, so skip it.
        testFixturesCodeElementsNames.forEach {
          javaComponent.withVariantsFromConfiguration(configurations[it]) { skip() }
        }
      } else {
        // Test-fixtures jar will have real contents, so add Javadoc and sources
        artifact(testFixturesJavadocJar)
        artifact(testFixturesSourcesJar)
      }

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
                  unsubscribe =
                      "https://sourceforge.net/projects/wala/lists/wala-$topic/unsubscribe"
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

val repositories: RepositoryHandler = publishing.repositories

val mavenRepository: MavenArtifactRepository =
    repositories.maven {
      url =
          uri(
              (if (isSnapshot)
                  properties.getOrDefault(
                      "SNAPSHOT_REPOSITORY_URL",
                      "https://oss.sonatype.org/content/repositories/snapshots/")
              else
                  properties.getOrDefault(
                      "RELEASE_REPOSITORY_URL",
                      "https://oss.sonatype.org/service/local/staging/deploy/maven2/"))
                  as String)
      credentials {
        username = findProperty("SONATYPE_NEXUS_USERNAME") as String?
        password = findProperty("SONATYPE_NEXUS_PASSWORD") as String?
      }
    }

repositories.maven {
  name = "fakeRemote"
  setUrl(rootProject.layout.buildDirectory.dir("maven-fake-remote-repository"))
}

signing {
  sign(mavenPublication)
  setRequired {
    // Signatures are a hard requirement if publishing a non-snapshot to a real, remote repository.
    !isSnapshot &&
        gradle.taskGraph.allTasks.any {
          it is PublishToMavenRepository && it.repository == mavenRepository
        }
  }
}

java {
  withJavadocJar()
  withSourcesJar()
}
