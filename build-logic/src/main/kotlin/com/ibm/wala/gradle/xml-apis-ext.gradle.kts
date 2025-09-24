package com.ibm.wala.gradle

configurations.all {
  // `org.eclipse.platform:org.eclipse.e4.ui.css.core:0.14.200`,
  // `org.eclipse.platform:org.eclipse.e4.ui.css.swt.theme:0.14.200`, and
  // `org.eclipse.platform:org.eclipse.e4.ui.css.swt:0.15.200` depend on
  // `xml-apis:xml-apis-ext:1.0.0.v20230923-0644`, which is not available in any configured
  // repository.
  //
  // `org.eclipse.platform:org.eclipse.e4.ui.css.core:0.14.200` also depends on
  // `org.apache.xmlgraphics:batik-css:1.17`, which depends on `xml-apis:xml-apis-ext:1.3.04`, which
  // is available from Maven Central.  So force that version of `xml-apis-ext` to be used instead of
  // the missing one.
  resolutionStrategy.force(catalogLibrary("xml-apis-ext"))
}
