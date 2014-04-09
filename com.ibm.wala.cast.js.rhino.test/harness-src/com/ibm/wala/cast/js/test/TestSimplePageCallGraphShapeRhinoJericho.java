package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.ibm.wala.cast.js.html.IHtmlParser;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.util.Util;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;


public class TestSimplePageCallGraphShapeRhinoJericho extends TestSimplePageCallGraphShapeRhino {

	@Test public void testCrawl() throws IOException, IllegalArgumentException, CancelException {
		URL url = getClass().getClassLoader().getResource("pages/crawl.html");
		CallGraph CG = JSCallGraphBuilderUtil.makeHTMLCG(url);
		verifyGraphAssertions(CG, null);
	}

	@Test public void testParseError() throws IOException, IllegalArgumentException, CancelException {
		URL url = getClass().getClassLoader().getResource("pages/garbage.html");
		JSCFABuilder B = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
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
