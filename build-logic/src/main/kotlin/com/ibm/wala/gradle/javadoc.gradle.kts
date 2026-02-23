package com.ibm.wala.gradle

// Build configuration for projects that include `Javadoc` tasks.

val javadocClasspath by
    configurations.registering { description = "Classpath used during Javadoc creation." }

tasks.named<Javadoc>("javadoc") { classpath = files(javadocClasspath) }

tasks.withType<Javadoc>().configureEach {
  with(options as StandardJavadocDocletOptions) {
    addBooleanOption("Xdoclint:all,-missing", true)
    encoding = "UTF-8"
    links(
        "https://docs.oracle.com/en/java/javase/${javadocTool.get().metadata.languageVersion}/docs/api/"
    )
    quiet()
    tags!!.add("apiNote:a:API Note:")
  }
}
