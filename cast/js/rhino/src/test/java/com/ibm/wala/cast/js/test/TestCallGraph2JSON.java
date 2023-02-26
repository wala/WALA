package com.ibm.wala.cast.js.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.CallGraph2JSON;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Before;
import org.junit.Test;

public class TestCallGraph2JSON {

  private FieldBasedCGUtil util;

  @Before
  public void setUp() throws Exception {
    util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
  }

  @Test
  public void testBasic() throws WalaException, CancelException {
    String script = "tests/fieldbased/simple.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(true);
    Map<String, Map<String, String[]>> parsedJSONCG = getParsedJSONCG(cg, cg2JSON);
    Set<String> methods = parsedJSONCG.keySet();
    assertEquals(3, methods.size());
    for (Entry<String, Map<String, String[]>> entry : parsedJSONCG.entrySet()) {
      if (entry.getKey().startsWith("simple.js@3")) {
        Map<String, String[]> callSites = entry.getValue();
        assertThat(
            Arrays.asList(getTargetsStartingWith(callSites, "simple.js@4")),
            hasItemStartingWith("simple.js@7"));
      }
    }
    Map<String, String[]> flattened = flattenParsedCG(parsedJSONCG);
    assertEquals(5, flattened.keySet().size());
    flattened.values().stream().forEach(callees -> assertEquals(1, callees.length));
  }

  @Test
  public void testNative() throws WalaException, CancelException {
    String script = "tests/fieldbased/native_call.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false);
    Map<String, String[]> parsed = getFlattenedJSONCG(cg, cg2JSON);
    assertArrayEquals(
        new String[] {"Array_prototype_pop (Native)"},
        getTargetsStartingWith(parsed, "native_call.js@2"));
  }

  @Test
  public void testReflectiveCalls() throws WalaException, CancelException {
    String script = "tests/fieldbased/reflective_calls.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false, true);
    Map<String, String[]> parsed = getFlattenedJSONCG(cg, cg2JSON);
    assertThat(
        Arrays.asList(getTargetsStartingWith(parsed, "reflective_calls.js@10")),
        hasItemStartingWith("Function_prototype_call (Native) [reflective_calls.js@10"));
    assertThat(
        Arrays.asList(getTargetsStartingWith(parsed, "reflective_calls.js@11")),
        hasItemStartingWith("Function_prototype_apply (Native) [reflective_calls.js@11"));
    assertThat(
        Arrays.asList(
            getTargetsStartingWith(
                parsed, "Function_prototype_call (Native) [reflective_calls.js@10")),
        hasItemStartingWith("reflective_calls.js@1"));
    assertThat(
        Arrays.asList(
            getTargetsStartingWith(
                parsed, "Function_prototype_apply (Native) [reflective_calls.js@11")),
        hasItemStartingWith("reflective_calls.js@5"));
  }

  @Test
  public void testNativeCallback() throws WalaException, CancelException {
    String script = "tests/fieldbased/native_callback.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false);
    Map<String, String[]> parsed = getFlattenedJSONCG(cg, cg2JSON);
    assertArrayEquals(
        new String[] {"Array_prototype_map (Native)"},
        getTargetsStartingWith(parsed, "native_callback.js@2"));
    assertThat(
        Arrays.asList(getTargetsStartingWith(parsed, "Function_prototype_call (Native)")),
        hasItemStartingWith("native_callback.js@3"));
  }

  /**
   * returns a parsed version of the JSON of the call graph, but flattened to just be a map from
   * call sites to targets (stripping out the outermost level of containing methods)
   */
  private static Map<String, String[]> getFlattenedJSONCG(CallGraph cg, CallGraph2JSON cg2JSON) {
    Map<String, Map<String, String[]>> parsed = getParsedJSONCG(cg, cg2JSON);
    return flattenParsedCG(parsed);
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

  private CallGraph buildCallGraph(String script) throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    return util.buildCG(
            scriptURL, BuilderType.OPTIMISTIC_WORKLIST, null, false, DefaultSourceExtractor::new)
        .getCallGraph();
  }

  /**
   * We need this method since column offsets can differ across platforms, so we can't do an exact
   * position match
   */
  private static String[] getTargetsStartingWith(Map<String, String[]> parsedJSON, String prefix) {
    for (Entry<String, String[]> entry : parsedJSON.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        return entry.getValue();
      }
    }
    throw new RuntimeException(prefix + " not a key prefix");
  }

  /**
   * We need this method since column offsets can differ across platforms, so we can't do an exact
   * position match
   */
  private static IsIterableContaining<String> hasItemStartingWith(String prefix) {
    return new IsIterableContaining<>(startsWith(prefix));
  }
}
