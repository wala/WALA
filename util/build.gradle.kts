plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.NullAway")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  api(libs.jspecify)
  compileOnly(libs.jetbrains.annotations)
  javadocClasspath(projects.core)
  testFixturesApi(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
}

tasks.named<Javadoc>("javadoc") {
  val currentJavaVersion = JavaVersion.current()
  val linksPrefix = if (currentJavaVersion >= JavaVersion.VERSION_11) "en/java/" else ""
  (options as StandardJavadocDocletOptions).run {
    links(
        "https://docs.oracle.com/${linksPrefix}javase/${currentJavaVersion.majorVersion}/docs/api/")
    source = "8" // workaround https://bugs.openjdk.java.net/browse/JDK-8212233.
  }
}
