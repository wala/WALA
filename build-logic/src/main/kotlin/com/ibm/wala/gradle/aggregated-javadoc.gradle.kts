/**
 * [Configuration]s representing subprojects' contributions toward project-wide Javadoc.
 *
 * *Every* project and subproject defines these configurations. A subproject with no Java code
 * simply leaves these configuations empty. Defining these configurations everywhere simplifies
 * logic in the root project, since it can aggregate these from all subprojects without needing to
 * carefully filter out non-Java subprojects.
 */
package com.ibm.wala.gradle

val javadocClasspath by
    configurations.creating { description = "Classpath used during Javadoc creation." }

val javadocSource by
    configurations.creating {
      description = "Java source files from which Javadoc should be extracted."
    }
