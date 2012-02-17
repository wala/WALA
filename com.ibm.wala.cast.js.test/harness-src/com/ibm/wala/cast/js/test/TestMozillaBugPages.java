package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;

public abstract class TestMozillaBugPages extends TestJSCallGraphShape {

  public static void main(String[] args) {
    justThisTest(TestMozillaBugPages.class);
  }
    
  @Before
  public void config() {
    JSSourceExtractor.USE_TEMP_NAME = true;
    JSSourceExtractor.DELETE_UPON_EXIT = false;
  }

  @Test public void testMozilla439164() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/mochitest/mozillaBug439164.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Test public void testMozilla488233() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/mochitest/mozillaBug488233NoExtJS.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Test public void testMozilla490152() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/mochitest/mozillaBug490152NoExtJS.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Test public void testMozilla625562() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/mochitest/mozillaBug625562NoExtJS.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

}
