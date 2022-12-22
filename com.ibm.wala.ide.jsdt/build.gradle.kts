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
            "org.eclipse.core.jobs",
            "org.eclipse.core.resources",
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.common",
            "org.eclipse.osgi",
            "org.eclipse.ui.workbench",
        )
        .forEach { implementation(it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  api(project(":com.ibm.wala.ide")) {
    because("public class JavaScriptHeadlessUtil extends class HeadlessUtil")
  }
  val eclipseWstJsdtVersion: String by rootProject.extra
  implementation("org.eclipse.wst.jsdt:core:$eclipseWstJsdtVersion")
  implementation("org.eclipse.wst.jsdt:ui:$eclipseWstJsdtVersion")
  implementation(project(":com.ibm.wala.cast"))
  implementation(project(":com.ibm.wala.cast.js"))
  implementation(project(":com.ibm.wala.cast.js.rhino"))
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.util"))
  implementation("javax.annotation:javax.annotation-api") { version { strictly("1.3.2") } }
  testFixturesImplementation("javax.annotation:javax.annotation-api") {
    version { strictly("1.3.2") }
  }
}
