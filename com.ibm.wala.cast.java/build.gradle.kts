plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))
  testFixturesImplementation(libs.junit)
  testFixturesImplementation(project(":com.ibm.wala.cast"))
  testFixturesImplementation(project(":com.ibm.wala.core"))
}
