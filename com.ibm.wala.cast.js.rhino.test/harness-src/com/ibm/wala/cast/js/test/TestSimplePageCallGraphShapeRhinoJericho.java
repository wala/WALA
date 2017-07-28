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
package com.ibm.wala.cast.js.test;

import java.net.URL;

import org.junit.Test;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.util.Util;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;


public class TestSimplePageCallGraphShapeRhinoJericho extends TestSimplePageCallGraphShapeRhino {

	@Test public void testCrawl() throws IllegalArgumentException, CancelException, WalaException {
		URL url = getClass().getClassLoader().getResource("pages/crawl.html");
		CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url, DefaultSourceExtractor.factory);
		verifyGraphAssertions(CG, null);
	}

	@Test public void testParseError() throws IllegalArgumentException, CancelException, WalaException {
		URL url = getClass().getClassLoader().getResource("pages/garbage.html");
		JSCFABuilder B = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url, DefaultSourceExtractor.factory);
		B.makeCallGraph(B.getOptions());
	    Util.checkForFrontEndErrors(B.getClassHierarchy());
	}

	public static void main(String[] args) {
		justThisTest(TestSimplePageCallGraphShapeRhinoJericho.class);
	}

	@Override
	protected IHtmlParser getParser() {
		return new JerichoHtmlParser();
	}

}
