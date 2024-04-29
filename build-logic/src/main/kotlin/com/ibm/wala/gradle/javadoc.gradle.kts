package com.ibm.wala.gradle

// Build configuration for projects that include `Javadoc` tasks.

val javadocClasspath: Configuration by
    configurations.creating { description = "Classpath used during Javadoc creation." }

val javadocSource: Configuration by
    configurations.creating {
      description = "Java source files from which Javadoc should be extracted."
    }

tasks.named<Javadoc>("javadoc") {
  classpath = configurations.named("javadocClasspath").get()
  source(configurations.named("javadocSource"))
}

tasks.withType<Javadoc>().configureEach {
  with(options as StandardJavadocDocletOptions) {
    addBooleanOption("Xdoclint:all,-missing", true)
    encoding = "UTF-8"
    quiet()
    tags!!.add("apiNote:a:API Note:")
  }
}
