package com.ibm.wala.gradle

import java.net.URI

plugins {
  `java-library`
  `java-test-fixtures`
  `maven-publish`
  signing
  id("com.ibm.wala.gradle.subproject")
}

val isSnapshot = "SNAPSHOT" in version as String

val javaComponent = components["java"] as AdhocComponentWithVariants

val allTestFixturesSource = the<SourceSetContainer>()["testFixtures"].allSource

val testFixturesJavadoc by
    tasks.existing(Javadoc::class) {
      setDestinationDir(project.the<JavaPluginExtension>().docsDir.get().dir(name).asFile)
    }

val testFixturesJavadocJar by
    tasks.registering(Jar::class) {
      archiveClassifier.set("test-fixtures-javadoc")
      from(testFixturesJavadoc.map { it.destinationDir!! })
    }

val testFixturesSourcesJar by
    tasks.registering(Jar::class) {
      archiveClassifier.set("test-fixtures-sources")
      from(allTestFixturesSource)
    }

val publishing: PublishingExtension by extensions

fun createPublication(publicationName: String) =
    publishing.publications.create<MavenPublication>(publicationName) {
      from(javaComponent)

      val testFixturesCodeElementsNames =
          listOf("testFixturesApiElements", "testFixturesRuntimeElements")
      testFixturesCodeElementsNames.forEach(this::suppressPomMetadataWarningsFor)

      if (allTestFixturesSource.isEmpty()) {
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
        name.set(project.properties["POM_NAME"] as String)
        description.set("T. J. Watson Libraries for Analysis")
        inceptionYear.set("2006")
        url.set("https://github.com/wala/WALA")
        val pomUrl = url

        ciManagement {
          system.set("GitHub Actions")
          url.set("https://github.com/wala/WALA/actions")
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
                  id.set(entry.key)
                  name.set(entry.value)
                  url.set("https://github.com/{$entry.key}")
                }
              }
        }

        issueManagement {
          system.set("GitHub")
          url.set(pomUrl.map { "$it/issues" })
        }

        licenses {
          license {
            name.set("Eclipse Public License v2.0")
            url.set(pomUrl.map { "$it/blob/master/LICENSE" })
          }
        }

        mailingLists {
          listOf(
                  "commits",
                  "wala",
              )
              .forEach { topic ->
                mailingList {
                  name.set("wala-$topic")
                  archive.set("https://sourceforge.net/p/wala/mailman/wala-$topic")
                  subscribe.set("https://sourceforge.net/projects/wala/lists/wala-$topic")
                  unsubscribe.set(
                      "https://sourceforge.net/projects/wala/lists/wala-$topic/unsubscribe")
                  post.set("wala-$topic@lists.sourceforge.net")
                }
              }
        }

        scm {
          url.set(pomUrl)
          connection.set("scm:git:git://github.com/wala/WALA.git")
          developerConnection.set("scm:git:ssh://git@github.com/wala/WALA.git")
        }
      }
    }

val localPublication = createPublication("local")
val remotePublication = createPublication("remote")

publishing.repositories {
  maven {
    url =
        URI(
            (if (isSnapshot)
                project.properties.getOrDefault(
                    "SNAPSHOT_REPOSITORY_URL",
                    "https://oss.sonatype.org/content/repositories/snapshots/")
            else
                project.properties.getOrDefault(
                    "RELEASE_REPOSITORY_URL",
                    "https://oss.sonatype.org/service/local/staging/deploy/maven2/"))
                as String)
    credentials {
      username = project.properties["SONATYPE_NEXUS_USERNAME"] as String?
      password = project.properties["SONATYPE_NEXUS_PASSWORD"] as String?
    }
  }

  maven {
    name = "fakeRemote"
    url = uri("file://${rootProject.buildDir}/maven-fake-remote-repository")
  }
}

configure<SigningExtension> {
  // If anything about signing is misconfigured, fail loudly rather than quietly continuing with
  // unsigned artifacts.
  isRequired = true

  // Only sign publications sent to remote repositories; local install installatios are unsigned.
  // The `sign` invocation below causes eager creation of three tasks per subproject:
  // `signRemotePublication` is created immediately and `generateMetadataFileForRemotePublication`
  // and `generatePomFileForRemotePublication` are created during configuration.  Creating these
  // lazily instead will require a fix to <https://github.com/gradle/gradle/issues/8796>.
  sign(remotePublication)
}

// Only sign releases; snapshots are unsigned.
tasks.withType<Sign>().configureEach { onlyIf { !isSnapshot } }

java {
  withJavadocJar()
  withSourcesJar()
}

// Remote publication set goes to remote repositories.
tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf { publication == remotePublication }
}

// Local publication set goes to local installations.
tasks.withType<PublishToMavenLocal>().configureEach { onlyIf { publication == localPublication } }
