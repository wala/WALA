/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.test;

import java.net.URL;

import junit.framework.Assert;

import com.ibm.wala.cast.js.util.WebUtil;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.core.tests.util.WalaTestCase;

public class TestWebUtil extends WalaTestCase {

  public void testAjaxslt() {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xslt.html");
    Assert.assertTrue(url != null);
    Module mod = WebUtil.extractScriptFromHTML( url );
    Assert.assertTrue(mod != null);
  }

  public void testAjaxpath() {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xpath.html");
    Assert.assertTrue(url != null);
    Module mod = WebUtil.extractScriptFromHTML( url );
    Assert.assertTrue(mod != null);
  }
}
