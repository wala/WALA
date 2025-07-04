plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
  id("jacoco-report-aggregation")
}

(rootProject.subprojects - project).forEach {
  evaluationDependsOn(it.path)
  it.pluginManager.withPlugin("jacoco") { dependencies.jacocoAggregation(it) }
}
