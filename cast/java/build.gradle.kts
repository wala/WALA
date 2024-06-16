plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  api(projects.cast)
  api(projects.core)
  api(projects.util)
  implementation(projects.shrike)
  testFixturesApi(libs.junit.jupiter.api)
  testFixturesApi(projects.core)
  testFixturesApi(projects.util)
  testFixturesImplementation(projects.cast)
  testFixturesImplementation(projects.shrike)
}
