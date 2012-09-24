package com.ibm.wala.ide.test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;

public class WLProjectScopeTest extends AbstractJSProjectScopeTest {

  public static final String PROJECT_NAME = "HelloWorklightWorld";

  public static final String PROJECT_ZIP = "wl.zip";
  
  public static final ZippedProjectData PROJECT = new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);
  
  public WLProjectScopeTest() {
    super(PROJECT);
  }

}
