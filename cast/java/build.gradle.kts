plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  api(projects.cast)
  api(projects.core)
  api(projects.util)
  compileOnly(libs.jetbrains.annotations)
  implementation(projects.shrike)
  testFixturesApi(libs.junit.jupiter.api)
  testFixturesApi(libs.junit.jupiter.params)
  testFixturesApi(projects.core)
  testFixturesApi(projects.util)
  testFixturesImplementation(libs.assertj.core)
  testFixturesImplementation(projects.cast)
  testFixturesImplementation(projects.shrike)
}
