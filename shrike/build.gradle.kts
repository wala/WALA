plugins {
  id("com.ibm.wala.gradle.java")
  id("com.ibm.wala.gradle.publishing")
}

walaPublishing.pomName = "WALA Shrike"

eclipse.project.natures("org.eclipse.pde.PluginNature")

dependencies {
  api(libs.jspecify)
  api(projects.util)
}
