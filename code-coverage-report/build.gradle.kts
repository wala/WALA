plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("jacoco-report-aggregation")
}

dependencies {
  implementation(projects.cast.java)
  implementation(projects.cast.java.ecj)
  implementation(projects.cast.js)
  implementation(projects.cast.js.html.nuValidator)
  implementation(projects.cast.js.nodejs)
  implementation(projects.cast.js.rhino)
  implementation(projects.core)
  implementation(projects.dalvik)
  implementation(projects.ide)
  implementation(projects.ide.jdt)
  implementation(projects.ide.jdt.test)
  implementation(projects.ide.jsdt)
  implementation(projects.ide.jsdt.tests)
  implementation(projects.ide.tests)
  implementation(projects.scandroid)
  implementation(projects.shrike)
  implementation(projects.util)
}
