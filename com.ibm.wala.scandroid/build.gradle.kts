plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  implementation(libs.commons.cli)
  implementation(libs.guava)
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.dalvik"))
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))
}
