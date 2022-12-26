plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
}

repositories {
  maven {
    url = uri("https://artifacts.alfresco.com/nexus/content/repositories/public/")
    content { includeGroup("org.eclipse.wst.jsdt") }
  }
}

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    listOf(
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.common",
            "org.eclipse.osgi",
        )
        .forEach { dep("testImplementation", it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  testImplementation(
      "org.eclipse.wst.jsdt:core:${rootProject.extra["eclipseWstJsdtVersion"] as String}")
  testImplementation("org.eclipse.platform:org.eclipse.osgi:3.15.100")
  testImplementation(project(":com.ibm.wala.cast"))
  testImplementation(project(":com.ibm.wala.cast.js"))
  testImplementation(project(":com.ibm.wala.cast.js.rhino"))
  testImplementation(project(":com.ibm.wala.core"))
  testImplementation(project(":com.ibm.wala.ide.jsdt"))
  testImplementation(project(":com.ibm.wala.util"))
  testImplementation(project(configuration = "testArchives", path = ":com.ibm.wala.ide.tests"))
  testImplementation("javax.annotation:javax.annotation-api") { version { strictly("1.3.2") } }
}

tasks.named<Test>("test") {
  // https://github.com/liblit/WALA/issues/5
  exclude("**/JSProjectScopeTest.class")
}
