plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("jacoco-report-aggregation")
}

dependencies {
  rootProject.allprojects { plugins.withId("jacoco") { jacocoAggregation(this@allprojects) } }
}
