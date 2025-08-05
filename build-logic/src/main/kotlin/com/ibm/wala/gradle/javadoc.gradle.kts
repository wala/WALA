package com.ibm.wala.gradle

// Build configuration for projects that include `Javadoc` tasks.

val javadocClasspath by
    configurations.registering { description = "Classpath used during Javadoc creation." }

val javadocSource by
    configurations.registering {
      description = "Java source files from which Javadoc should be extracted."
    }

tasks.named<Javadoc>("javadoc") {
  classpath = javadocClasspath.get()
  source(javadocSource)
}

tasks.withType<Javadoc> {
  with(options as StandardJavadocDocletOptions) {
    addBooleanOption("Xdoclint:all,-missing", true)
    encoding = "UTF-8"
    quiet()
    tags!!.add("apiNote:a:API Note:")
  }
}
