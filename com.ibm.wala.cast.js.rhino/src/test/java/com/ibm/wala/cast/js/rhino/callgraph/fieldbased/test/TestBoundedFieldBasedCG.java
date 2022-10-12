package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.test.TestCallGraph2JSON;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.CallGraph2JSON;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.core.util.ProgressMaster;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Before;
import org.junit.Test;

public class TestBoundedFieldBasedCG{

  private FieldBasedCGUtil util;

  @Before
  public void setUp() throws Exception {
    util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
  }

  /*@Test
  public void testBoundedWorklistBound1() throws WalaException, Error, CancelException {
    runTest("tests/fieldbased/bounded.js", assertionsForSimpleJS, BuilderType.OPTIMISTIC_WORKLIST);
  }**/
  @Test
  public void testBoundedCalls() throws WalaException, CancelException {
    String script = "tests/fieldbased/bounded.js";

    CallGraph cg = buildBoundedCallGraph(script, 3);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false, true);
    Map<String, String[]> parsed = getFlattenedJSONCG(cg, cg2JSON);
    assertThat(
        Arrays.asList(getTargetsStartingWith(parsed, "bounded.js@5:144-147")),
        hasItemStartingWith("bounded.js@3:89-114"));
  }

  private static Map<String, String[]> flattenParsedCG(Map<String, Map<String, String[]>> parsed) {
    Map<String, String[]> result = HashMapFactory.make();
    for (Map<String, String[]> siteInfo : parsed.values()) {
      result.putAll(siteInfo);
    }
    return result;
  }

  private static Map<String, Map<String, String[]>> getParsedJSONCG(
      CallGraph cg, CallGraph2JSON cg2JSON) {
    String json = cg2JSON.serialize(cg);
    // System.err.println(json);
    Gson gson = new Gson();
    Type mapType = new TypeToken<Map<String, Map<String, String[]>>>() {}.getType();
    return gson.fromJson(json, mapType);
  }
  private static Map<String, String[]> getFlattenedJSONCG(CallGraph cg, CallGraph2JSON cg2JSON) {
    Map<String, Map<String, String[]>> parsed = getParsedJSONCG(cg, cg2JSON);
    return flattenParsedCG(parsed);
  }

  private static String[] getTargetsStartingWith(Map<String, String[]> parsedJSON, String prefix) {
    for (Entry<String, String[]> entry : parsedJSON.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        return entry.getValue();
      }
    }
    //throw new RuntimeException(prefix + " not a key prefix");
    throw new RuntimeException(parsedJSON.toString());
  }
  private static IsIterableContaining<String> hasItemStartingWith(String prefix) {
    return new IsIterableContaining<>(startsWith(prefix));
  }
  private CallGraph buildBoundedCallGraph(String script, int bound) throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(new CAstRhinoTranslatorFactory());
    IProgressMonitor monitor = ProgressMaster.make(new NullProgressMonitor(), 45000, true);
    List<Module> scripts = new ArrayList<>();
    scripts.add(new SourceURLModule(scriptURL));
    scripts.add(JSCallGraphUtil.getPrologueFile("prologue.js"));
    return util.buildBoundedCG(
        loaders, scripts.toArray(new Module[0]), monitor, false, bound).getCallGraph();
  }

}
