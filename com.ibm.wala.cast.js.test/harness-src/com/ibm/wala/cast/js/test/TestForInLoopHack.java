package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.ForInContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.util.CancelException;

public abstract class TestForInLoopHack extends TestJSCallGraphShape {

  @Before
  public void config() {
    JSSourceExtractor.USE_TEMP_NAME = true;
    JSSourceExtractor.DELETE_UPON_EXIT = false;
  }

  @Test public void testPage3WithoutHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Test public void testPage3WithHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
  }

  @Ignore("This test now blows up due to proper handling of the || construct, used in extend().  Should handle this eventually.")
  @Test public void testJQueryWithHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/jquery_hacked.html");
    JSCFABuilder builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    JSCallGraphBuilderUtil.dumpCG(builder.getPointerAnalysis(), CG);
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
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    JSCallGraphBuilderUtil.dumpCG(B.getPointerAnalysis(), CG);
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
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    JSCallGraphBuilderUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForBadForin);
    verifyGraphAssertions(CG, assertionsForBadForinHackPrecision);
  }

  private static final Object[][] assertionsForbadforin2 = new Object[][] { 
    new Object[] { ROOT, 
      new String[] { "tests/badforin2.js" } },
    new Object[] { "tests/badforin2.js", 
      new String[] { "tests/badforin2.js/testForIn", "tests/badforin2.js/_check_obj_foo", "tests/badforin2.js/_check_obj_bar", "tests/badforin2.js/_check_copy_foo", "tests/badforin2.js/_check_copy_bar"} },
    new Object[] { "tests/badforin2.js/testForIn",
      new String[] { "tests/badforin2.js/testForIn1", "tests/badforin2.js/testForIn2" } },
    new Object[] { "tests/badforin2.js/_check_obj_foo",
      new String[] { "tests/badforin2.js/testForIn1" } },
    new Object[] { "tests/badforin2.js/_check_copy_foo",
      new String[] { "tests/badforin2.js/testForIn1" } },
    new Object[] { "tests/badforin2.js/_check_obj_bar",
      new String[] { "tests/badforin2.js/testForIn2" } },
    new Object[] { "tests/badforin2.js/_check_copy_bar",
      new String[] { "tests/badforin2.js/testForIn2" } }
  };

  @Test public void testbadforin2WithoutHack() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin2.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    JSCallGraphBuilderUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForbadforin2);
  }

  private static final Object[][] assertionsForbadforin2HackPrecision = new Object[][] { 
    new Object[] { "tests/badforin2.js/_check_obj_foo",
      new String[] { "!tests/badforin2.js/testForIn2" } },
    new Object[] { "tests/badforin2.js/_check_copy_foo",
      new String[] { "!tests/badforin2.js/testForIn2" } },
    new Object[] { "tests/badforin2.js/_check_obj_bar",
      new String[] { "!tests/badforin2.js/testForIn1" } },
    new Object[] { "tests/badforin2.js/_check_copy_bar",
      new String[] { "!tests/badforin2.js/testForIn1" } }
  };

  @Test public void testbadforin2WithHack() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin2.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    JSCallGraphBuilderUtil.dumpCG(B.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsForbadforin2);
    verifyGraphAssertions(CG, assertionsForbadforin2HackPrecision);
  }

  @Test public void testForInRecursion() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder B = JSCallGraphBuilderUtil.makeScriptCGBuilder("tests", "badforin3.js");
    addHackedForInLoopSensitivity(B);
    CallGraph CG = B.makeCallGraph(B.getOptions());
    JSCallGraphBuilderUtil.dumpCG(B.getPointerAnalysis(), CG);
  }


  /*
  @Test public void testYahooWithoutHack() throws IOException, IllegalArgumentException, CancelException {
    JSCFABuilder B = Util.makeScriptCGBuilder("frameworks", "yahoo.js");
    CallGraph CG = B.makeCallGraph(B.getOptions());
    Util.dumpCG(B.getPointerAnalysis(), CG);
  }
  */
  
  private void addHackedForInLoopSensitivity(JSCFABuilder builder) {
    final ContextSelector orig = builder.getContextSelector();
    builder.setContextSelector(new ForInContextSelector(orig));
  }

}
