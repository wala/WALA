/** Define a Maven repository containing Eclipse JSDT packages. */
package com.ibm.wala.gradle

repositories.maven {
  url = uri("https://artifacts.alfresco.com/nexus/content/repositories/public/")
  content { includeGroup("org.eclipse.wst.jsdt") }
}
