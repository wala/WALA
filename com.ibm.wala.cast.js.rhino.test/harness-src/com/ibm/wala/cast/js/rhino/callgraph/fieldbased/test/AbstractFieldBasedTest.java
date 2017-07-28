package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.junit.Before;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil;
import com.ibm.wala.cast.js.test.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.cast.js.test.TestJSCallGraphShape;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.CallGraph2JSON;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.ProgressMaster;
import com.ibm.wala.util.WalaException;

public abstract class AbstractFieldBasedTest extends TestJSCallGraphShape {

  protected FieldBasedCGUtil util;

  public AbstractFieldBasedTest() {
    super();
  }

  @Override
  @Before
  public void setUp() throws Exception {
  	util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
  }

  protected JSCallGraph runTest(String script, Object[][] assertions, BuilderType... builderTypes) throws WalaException, Error, CancelException {
     return runTest(TestFieldBasedCG.class.getClassLoader().getResource(script), assertions, builderTypes);
   }

  protected JSCallGraph runTest(URL url, Object[][] assertions, BuilderType... builderTypes) throws WalaException, Error, CancelException {
    JSCallGraph cg = null;
    for(BuilderType builderType : builderTypes) {
      ProgressMaster monitor = ProgressMaster.make(new NullProgressMonitor(), 45000, true);
      try {
        cg = util.buildCG(url, builderType, monitor, false, DefaultSourceExtractor.factory).fst;
        System.err.println(cg);
        verifyGraphAssertions(cg, assertions);
      } catch(AssertionError afe) {
        throw new AssertionError(builderType + ": " + afe.getMessage());
      } 
    }
    return cg;
  }

  /**
   * for long-running tests that tend to time out on Travis
   */
  protected JSCallGraph runTestExceptOnTravis(URL url, Object[][] assertions, BuilderType... builderTypes) throws WalaException, Error, CancelException {
    if (System.getenv("TRAVIS") == null) {
      return runTest(url, assertions, builderTypes);
    } else {
      return null;
    }
  }

  protected void dumpCG(JSCallGraph cg) {
  	CallGraph2JSON.IGNORE_HARNESS = false;
  	Map<String, Set<String>> edges = CallGraph2JSON.extractEdges(cg);
  	for(String callsite : edges.keySet())
  		for(String callee : edges.get(callsite))
  			System.out.println(callsite + " -> " + callee);
  }

}
