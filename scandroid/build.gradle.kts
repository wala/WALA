plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  implementation(libs.commons.cli)
  implementation(libs.guava)
  implementation(projects.core)
  implementation(projects.dalvik)
  implementation(projects.shrike)
  implementation(projects.util)
}
