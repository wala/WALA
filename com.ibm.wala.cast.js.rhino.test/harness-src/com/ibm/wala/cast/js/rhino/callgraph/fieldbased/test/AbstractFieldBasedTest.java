package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.junit.Before;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test.CGUtil.BuilderType;
import com.ibm.wala.cast.js.test.TestJSCallGraphShape;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.CallGraph2JSON;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.ProgressMaster;
import com.ibm.wala.util.WalaException;

public abstract class AbstractFieldBasedTest extends TestJSCallGraphShape {

  protected CGUtil util;

  public AbstractFieldBasedTest() {
    super();
  }

  @Override
  @Before
  public void setUp() throws Exception {
  	util = new CGUtil(new CAstRhinoTranslatorFactory());
  }

  protected JSCallGraph runTest(String script, Object[][] assertions, BuilderType... builderTypes) throws IOException, WalaException, Error, CancelException {
     return runTest(TestFieldBasedCG.class.getClassLoader().getResource(script), assertions, builderTypes);
   }

  protected JSCallGraph runTest(URL url, Object[][] assertions, BuilderType... builderTypes) throws IOException, WalaException, Error, CancelException {
    JSCallGraph cg = null;
    for(BuilderType builderType : builderTypes) {
      ProgressMaster monitor = ProgressMaster.make(new NullProgressMonitor(), 30000, true);
      try {
        cg = util.buildCG(url, builderType, monitor);
        verifyGraphAssertions(cg, assertions);
      } catch(AssertionFailedError afe) {
        throw new AssertionFailedError(builderType + ": " + afe.getMessage());
      } 
    }
    return cg;
  }

  @SuppressWarnings("unused")
  private void dumpCG(JSCallGraph cg) {
  	CallGraph2JSON.IGNORE_HARNESS = false;
  	Map<String, Set<String>> edges = CallGraph2JSON.extractEdges(cg);
  	for(String callsite : edges.keySet())
  		for(String callee : edges.get(callsite))
  			System.out.println(callsite + " -> " + callee);
  }

}