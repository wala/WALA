import com.ibm.wala.gradle.forEachJavaProject

plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("jacoco-report-aggregation")
}

forEachJavaProject { dependencies.jacocoAggregation(dependencies.project(it.path)) }
