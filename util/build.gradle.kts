plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.NullAway")
  id("com.ibm.wala.gradle.publishing")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  api(libs.json)
  api(libs.jspecify)
  compileOnly(libs.jetbrains.annotations)
  implementation(libs.guava)
  javadocClasspath(projects.core)
  testFixturesApi(libs.assertj.core)
  testFixturesApi(libs.jspecify)
  testImplementation(libs.junit.jupiter.api)
}
