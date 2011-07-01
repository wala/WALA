package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.ForInContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.util.CancelException;

public class TestForInLoopHack extends TestJSCallGraphShape {

  @Before
  public void config() {
    JSSourceExtractor.USE_TEMP_NAME = false;
    JSSourceExtractor.DELETE_UPON_EXIT = false;
  }

  @Test public void testPage3WithoutHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    Util.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Test public void testPage3WithHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    Util.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Test public void testJQueryWithHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/jquery_hacked.html");
    JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    Util.dumpCG(builder.getPointerAnalysis(), CG);
  }

  /*
  @Test public void testJQueryEx1WithHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/jquery/ex1.html");
    JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    Util.dumpCG(builder.getPointerAnalysis(), CG);
  }
  */
  
  private static final Object[][] assertionsForBadForin = new Object[][] { 
    new Object[] { ROOT, 
      new String[] { "tests/badforin.js" } },
    new Object[] { "tests/badforin.js", 
      new String[] { "tests/badforin.js/testForIn", "tests/badforin.js/_check_obj_foo", "tests/badforin.js/_check_obj_bar", "tests/badforin.js/_check_copy_foo", "tests/badforin.js/_check_copy_bar"} },
    new Object[] { "tests/badforin.js/testForIn",
      new String[] { "tests/badforin.js/testForIn1", "tests/badforin.js/testForIn2" } },
    new Object[] { "tests/badforin.js/_check_obj_foo",
      new String[] { "tests/badforin.js/testForIn1" } },
    new Object[] { "tests/badforin.js/_check_copy_foo",
      new String[] { "tests/badforin.js/testForIn1" } },
    new Object[] { "tests/badforin.js/_check_obj_bar",
      new String[] { "tests/badforin.js/testForIn2" } },
    new Object[] { "tests/badforin.js/_check_copy_bar",
      new String[] { "tests/badforin.js/testForIn2" } }
  };

  @Test public void testBadForInWithoutHack() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder B = Util.makeScriptCGBuilder("tests", "badforin.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    Util.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForBadForin);
  }

  private static final Object[][] assertionsForBadForinHackPrecision = new Object[][] { 
    new Object[] { "tests/badforin.js/_check_obj_foo",
      new String[] { "!tests/badforin.js/testForIn2" } },
    new Object[] { "tests/badforin.js/_check_copy_foo",
      new String[] { "!tests/badforin.js/testForIn2" } },
    new Object[] { "tests/badforin.js/_check_obj_bar",
      new String[] { "!tests/badforin.js/testForIn1" } },
    new Object[] { "tests/badforin.js/_check_copy_bar",
      new String[] { "!tests/badforin.js/testForIn1" } }
  };

  @Test public void testBadForInWithHack() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder B = Util.makeScriptCGBuilder("tests", "badforin.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    Util.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForBadForin);
    verifyGraphAssertions(CG, assertionsForBadForinHackPrecision);
  }
  
  private void addHackedForInLoopSensitivity(JSCFABuilder builder) {
    final ContextSelector orig = builder.getContextSelector();
    builder.setContextSelector(new DelegatingContextSelector(new ForInContextSelector(), orig));
  }

}
