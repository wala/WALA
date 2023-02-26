plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  implementation(projects.cast)
  implementation(projects.core)
  implementation(projects.shrike)
  implementation(projects.util)
  testFixturesImplementation(libs.junit)
  testFixturesImplementation(projects.cast)
  testFixturesImplementation(projects.core)
}
