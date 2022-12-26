package com.ibm.wala.gradle

import org.gradle.kotlin.dsl.withType

// Build configuration for projects that include `Javadoc` tasks.

tasks.withType<Javadoc>().configureEach {
  with(options as StandardJavadocDocletOptions) {
    addBooleanOption("Xdoclint:all,-missing", true)
    encoding = "UTF-8"
    quiet()
    tags!!.add("apiNote:a:API Note:")
  }
}
