plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("jacoco-report-aggregation")
}

dependencies {
  jacocoAggregation(projects.cast.java)
  jacocoAggregation(projects.cast.java.ecj)
  jacocoAggregation(projects.cast.js)
  jacocoAggregation(projects.cast.js.html.nuValidator)
  jacocoAggregation(projects.cast.js.nodejs)
  jacocoAggregation(projects.cast.js.rhino)
  jacocoAggregation(projects.core)
  jacocoAggregation(projects.dalvik)
  jacocoAggregation(projects.ide)
  jacocoAggregation(projects.ide.jdt)
  jacocoAggregation(projects.ide.jdt.test)
  jacocoAggregation(projects.ide.jsdt)
  jacocoAggregation(projects.ide.jsdt.tests)
  jacocoAggregation(projects.ide.tests)
  jacocoAggregation(projects.scandroid)
  jacocoAggregation(projects.shrike)
  jacocoAggregation(projects.util)
}
