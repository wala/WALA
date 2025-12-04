plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.jmh")
  id("com.ibm.wala.gradle.NullAway")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  api(libs.jspecify)
  compileOnly(libs.jetbrains.annotations)
  javadocClasspath(projects.core)
  jmhImplementation(libs.assertj.core)
  testFixturesApi(libs.assertj.core)
  testImplementation(libs.junit.jupiter.api)
}
