package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assume.assumeThat;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.test.TestJSCallGraphShape;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.CallGraph2JSON;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.core.util.ProgressMaster;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;

public abstract class AbstractFieldBasedTest extends TestJSCallGraphShape {

  protected FieldBasedCGUtil util;

  public AbstractFieldBasedTest() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
  }

  protected JSCallGraph runTest(String script, Object[][] assertions, BuilderType... builderTypes)
      throws WalaException, Error, CancelException {
    return runTest(
        TestFieldBasedCG.class.getClassLoader().getResource(script), assertions, builderTypes);
  }

  protected JSCallGraph runTest(URL url, Object[][] assertions, BuilderType... builderTypes)
      throws WalaException, Error, CancelException {
    JSCallGraph cg = null;
    for (BuilderType builderType : builderTypes) {
      IProgressMonitor monitor = ProgressMaster.make(new NullProgressMonitor(), 45000, true);
      try {
        cg =
            util.buildCG(url, builderType, monitor, false, DefaultSourceExtractor.factory)
                .getCallGraph();
        System.err.println(cg);
        verifyGraphAssertions(cg, assertions);
      } catch (AssertionError afe) {
        throw new AssertionError(builderType + ": " + afe.getMessage());
      }
    }
    return cg;
  }

  protected JSCallGraph runBoundedTest(
      String script, Object[][] assertions, BuilderType builderType, int bound)
      throws WalaException, Error, CancelException {
    JSCallGraph cg = null;
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(new CAstRhinoTranslatorFactory());
    IProgressMonitor monitor = ProgressMaster.make(new NullProgressMonitor(), 45000, true);
    List<Module> scripts = new ArrayList<>();
    URL url = TestFieldBasedCG.class.getClassLoader().getResource(script);
    scripts.add(new SourceURLModule(url));
    scripts.add(JSCallGraphUtil.getPrologueFile("prologue.js"));
    try {
      cg =
          util.buildBoundedCG(loaders, scripts.toArray(new Module[0]), monitor, false, bound)
              .getCallGraph();
      System.err.println(cg);
      verifyGraphAssertions(cg, assertions);
    } catch (AssertionError afe) {
      throw new AssertionError(builderType + ": " + afe.getMessage(), afe);
    }
    return cg;
  }

  /** for long-running tests that tend to time out on Travis */
  protected void runTestExceptOnTravis(URL url, Object[][] assertions, BuilderType... builderTypes)
      throws WalaException, Error, CancelException {
    assumeThat("not running on Travis CI", System.getenv("TRAVIS"), nullValue());
    runTest(url, assertions, builderTypes);
  }

  protected void dumpCG(JSCallGraph cg) {
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false);
    Map<String, Map<String, Set<String>>> edges = cg2JSON.extractEdges(cg);
    for (Map<String, Set<String>> sitesInMethod : edges.values()) {
      for (Map.Entry<String, Set<String>> entry : sitesInMethod.entrySet()) {
        for (String callee : entry.getValue()) {
          System.out.println(entry.getKey() + " -> " + callee);
        }
      }
    }
  }
}
