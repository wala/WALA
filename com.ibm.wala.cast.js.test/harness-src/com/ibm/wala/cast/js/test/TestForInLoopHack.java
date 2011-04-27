package com.ibm.wala.cast.js.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

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
    Util.dumpCG(builder, CG);
  }

  @Test public void testPage3WithHack() throws IOException, IllegalArgumentException, CancelException {
    URL url = getClass().getClassLoader().getResource("pages/page3.html");
    JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
    addHackedForInLoopSensitivity(builder);
    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    Util.dumpCG(builder, CG);
  }

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
    Util.dumpCG(B, CG);
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
    Util.dumpCG(B, CG);
    verifyGraphAssertions(CG, assertionsForBadForin);
    verifyGraphAssertions(CG, assertionsForBadForinHackPrecision);
  }
  
  private void addHackedForInLoopSensitivity(JSCFABuilder builder) {
    final ContextSelector orig = builder.getContextSelector();
    builder.setContextSelector(new ContextSelector() {
      public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, final InstanceKey[] receiver) {
        final Context origContext = orig.getCalleeTarget(caller, site, callee, receiver);
        if (callee.getDeclaringClass().getName().toString().contains("_forin_body")) {
          class ForInContext implements Context {
            private final InstanceKey obj = receiver[2];
            public ContextItem get(ContextKey name) {
              if (name.equals(ContextKey.PARAMETERS[2])) {
                return new FilteredPointerKey.SingleInstanceFilter(obj);
              } else {
                return origContext.get(name);
              }
            }
            @Override
            public int hashCode() {
              return obj.hashCode();
            }
            @Override
            public boolean equals(Object other) {
              return other != null &&
                  getClass().equals(other.getClass()) &&
                  obj.equals(((ForInContext)other).obj);
            }     
            @Override
            public String toString() {
              return "for in hack filter for " + obj;
            }
          };
          return new ForInContext();
        } else {
          return origContext;
        }
      }
      public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
        if (caller.getIR().getCalls(site)[0].getNumberOfUses() > 2) {
          return IntSetUtil.make(new int[]{2}).union(orig.getRelevantParameters(caller, site));
        } else {
          return orig.getRelevantParameters(caller, site);
        }
      }
    });
  }

}
