plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  api(projects.core)
  api(projects.util)
  implementation(libs.commons.cli)
  implementation(libs.guava)
  implementation(projects.dalvik)
  implementation(projects.shrike)
}
