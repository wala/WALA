/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.jsdt.tests;

import java.io.IOException;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;

public class JSProjectScopeTest extends AbstractJSProjectScopeTest {

  public static final String PROJECT_NAME = "com.ibm.wala.cast.js.test.data";

  public static final String PROJECT_ZIP = "test_js_project.zip";
  
  public static final ZippedProjectData PROJECT;
  static {
    try {
      PROJECT = new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public JSProjectScopeTest() {
    super(PROJECT);
  }

}
