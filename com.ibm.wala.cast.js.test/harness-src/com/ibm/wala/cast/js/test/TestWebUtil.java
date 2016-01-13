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
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.core.tests.util.WalaTestCase;

public class TestWebUtil extends WalaTestCase {

  @Test public void testAjaxslt() throws Error {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xslt.html");
    Assert.assertTrue(url != null);
    Set<MappedSourceModule> mod = WebUtil.extractScriptFromHTML( url, DefaultSourceExtractor.factory ).fst;
    Assert.assertTrue(mod != null);
  }

  @Test public void testAjaxpath() throws Error {
    URL url = getClass().getClassLoader().getResource("ajaxslt/test/xpath.html");
    Assert.assertTrue(url != null);
    Set<MappedSourceModule> mod = WebUtil.extractScriptFromHTML( url, DefaultSourceExtractor.factory ).fst;
    Assert.assertTrue(mod != null);
  }
}
