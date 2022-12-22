plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

dependencies {
  implementation("com.google.guava:guava:31.1-jre")
  implementation("commons-cli:commons-cli:1.5.0")
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.dalvik"))
  implementation(project(":com.ibm.wala.shrike"))
  implementation(project(":com.ibm.wala.util"))
}
