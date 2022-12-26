plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.NullAway")
  id("com.ibm.wala.gradle.publishing")
}

val extraJavadocClasspath: Configuration by configurations.creating { isCanBeConsumed = false }

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  compileOnly("org.jspecify:jspecify:0.3.0")
  extraJavadocClasspath(project(":com.ibm.wala.core"))
  testImplementation("junit:junit:4.13.2")
  testImplementation(
      "org.hamcrest:hamcrest:2.2",
  )
  testRuntimeOnly(testFixtures(project(":com.ibm.wala.core")))
}

tasks.named<Javadoc>("javadoc") {
  classpath += extraJavadocClasspath
  val currentJavaVersion = JavaVersion.current()
  val linksPrefix = if (currentJavaVersion >= JavaVersion.VERSION_11) "en/java/" else ""
  (options as StandardJavadocDocletOptions).run {
    links(
        "https://docs.oracle.com/${linksPrefix}javase/${currentJavaVersion.majorVersion}/docs/api/")
    source = "8" // workaround https://bugs.openjdk.java.net/browse/JDK-8212233.
  }
}
