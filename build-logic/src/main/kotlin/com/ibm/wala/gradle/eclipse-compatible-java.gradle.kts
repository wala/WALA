package com.ibm.wala.gradle

plugins { java }

// Make Eclipse-compatible Java information available as `project.eclipseCompatibleJava`.
extensions.create<EclipseCompatibleJavaExtension>("eclipseCompatibleJava")
