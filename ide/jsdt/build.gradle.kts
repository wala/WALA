plugins {
  id("com.ibm.wala.gradle.eclipse-maven-central")
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.maven-eclipse-jsdt")
}

walaEclipseMavenCentral.api(
    "org.eclipse.core.resources",
    "org.eclipse.core.runtime",
    "org.eclipse.equinox.common",
    "org.eclipse.osgi",
    "org.eclipse.ui.workbench",
)

dependencies {
  api(libs.eclipse.wst.jsdt.core)
  api(libs.osgi.framework)
  api(projects.cast.js)
  api(projects.core)
  api(projects.ide) { because("public class JavaScriptHeadlessUtil extends class HeadlessUtil") }
  api(projects.util)
  implementation(libs.eclipse.wst.jsdt.ui)
  implementation(projects.cast)
  implementation(projects.cast.js.rhino)
}
