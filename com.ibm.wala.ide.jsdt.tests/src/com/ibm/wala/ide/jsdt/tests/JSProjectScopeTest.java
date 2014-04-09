package com.ibm.wala.ide.jsdt.tests;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;

public class JSProjectScopeTest extends AbstractJSProjectScopeTest {

  public static final String PROJECT_NAME = "com.ibm.wala.cast.js.test.data";

  public static final String PROJECT_ZIP = "test_js_project.zip";
  
  public static final ZippedProjectData PROJECT = new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);
  
  public JSProjectScopeTest() {
    super(PROJECT);
  }

}
