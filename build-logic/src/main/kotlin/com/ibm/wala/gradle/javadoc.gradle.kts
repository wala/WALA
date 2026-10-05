package com.ibm.wala.gradle

import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE

// Build configuration for projects that include `Javadoc` tasks.

val javadocClasspath =
    configurations.register("javadocClasspath") {
      description = "Classpath used during Javadoc creation."
      attributes {
        attribute(CATEGORY_ATTRIBUTE, named(LIBRARY))
        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(JAR))
      }
    }

tasks.named<Javadoc>("javadoc") { classpath = files(javadocClasspath) }

tasks.withType<Javadoc>().configureEach {
  with(options as StandardJavadocDocletOptions) {
    addBooleanOption("Xdoclint:all,-missing", true)
    encoding = "UTF-8"
    quiet()
    tags!!.add("apiNote:a:API Note:")
  }

  val javadocLink =
      javadocTool.map {
        "https://docs.oracle.com/en/java/javase/${it.metadata.languageVersion}/docs/api/"
      }
  doFirst { with(options as StandardJavadocDocletOptions) { links(javadocLink.get()) } }
}
