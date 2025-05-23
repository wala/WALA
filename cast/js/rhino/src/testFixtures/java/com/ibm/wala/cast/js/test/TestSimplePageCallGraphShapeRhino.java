/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class TestSimplePageCallGraphShapeRhino extends TestSimplePageCallGraphShape {

  private static final Object[][] assertionsForPage3 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"page3.html"}},
        new Object[] {"page3.html", new String[] {"page3.html/__WINDOW_MAIN__"}}
      };

  @Test
  public void testPage3() throws IllegalArgumentException, CancelException, WalaException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url, DefaultSourceExtractor.factory);
    verifyGraphAssertions(CG, assertionsForPage3);
  }

  @Test
  public void testJSParseError() {
    assertThatThrownBy(
            () -> {
              URL url = getClass().getClassLoader().getResource("pages/garbage2.html");
              JSCFABuilder B =
                  JSCallGraphBuilderUtil.makeHTMLCGBuilder(url, DefaultSourceExtractor.factory);
              B.makeCallGraph(B.getOptions());
              com.ibm.wala.cast.util.Util.checkForFrontEndErrors(B.getClassHierarchy());
            })
        .isInstanceOf(WalaException.class);
  }

  @Override
  protected abstract IHtmlParser getParser();

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp();
    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(
        new CAstRhinoTranslatorFactory());
    WebUtil.setFactory(TestSimplePageCallGraphShapeRhino.this::getParser);
  }
}
