package com.ibm.wala.gradle

import org.gradle.kotlin.dsl.withType

plugins { id("com.ibm.wala.gradle.aggregated-javadoc") }

// Build configuration for projects that include `Javadoc` tasks.

tasks.withType<Javadoc>().configureEach {
  classpath = configurations.named("javadocClasspath").get()
  source(configurations.named("javadocSource"))
  with(options as StandardJavadocDocletOptions) {
    addBooleanOption("Xdoclint:all,-missing", true)
    encoding = "UTF-8"
    quiet()
    tags!!.add("apiNote:a:API Note:")
  }
}
