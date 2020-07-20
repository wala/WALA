package com.ibm.wala.cast.js.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertArrayEquals;

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
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import org.junit.Assert;
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
    Map<String, String[]> parsed = getParsedJSONCG(cg, cg2JSON);
    Assert.assertEquals(5, parsed.keySet().size());
    parsed.values().stream()
        .forEach(
            callees -> {
              Assert.assertEquals(1, callees.length);
            });
  }

  @Test
  public void testNative() throws WalaException, CancelException {
    String script = "tests/fieldbased/native_call.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false);
    Map<String, String[]> parsed = getParsedJSONCG(cg, cg2JSON);
    String[] targets = parsed.get("native_call.js@2:21-28");
    assertArrayEquals(new String[] {"Array_prototype_pop (Native)"}, targets);
  }

  @Test
  public void testReflectiveCalls() throws WalaException, CancelException {
    String script = "tests/fieldbased/reflective_calls.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false);
    Map<String, String[]> parsed = getParsedJSONCG(cg, cg2JSON);
    assertArrayEquals(
        new String[] {"Function_prototype_call (Native)"},
        parsed.get("reflective_calls.js@10:63-78"));
    assertArrayEquals(
        new String[] {"Function_prototype_apply (Native)"},
        parsed.get("reflective_calls.js@11:82-99"));
    assertThat(
        Arrays.asList(parsed.get("Function_prototype_call (Native)")),
        hasItem("reflective_calls.js@1:0-24"));
    assertThat(
        Arrays.asList(parsed.get("Function_prototype_apply (Native)")),
        hasItem("reflective_calls.js@5:26-44"));
  }

  @Test
  public void testNativeCallback() throws WalaException, CancelException {
    String script = "tests/fieldbased/native_callback.js";
    CallGraph cg = buildCallGraph(script);
    CallGraph2JSON cg2JSON = new CallGraph2JSON(false);
    Map<String, String[]> parsed = getParsedJSONCG(cg, cg2JSON);
    assertArrayEquals(
        new String[] {"Array_prototype_map (Native)"}, parsed.get("native_callback.js@2:21-56"));
    assertThat(
        Arrays.asList(parsed.get("Function_prototype_call (Native)")),
        hasItem("native_callback.js@2:27-55"));
  }

  private static Map<String, String[]> getParsedJSONCG(CallGraph cg, CallGraph2JSON cg2JSON) {
    String json = cg2JSON.serialize(cg);
    // System.err.println(json);
    Gson gson = new Gson();
    Type mapType = new TypeToken<Map<String, String[]>>() {}.getType();
    return gson.fromJson(json, mapType);
  }

  private CallGraph buildCallGraph(String script) throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    return util.buildCG(
            scriptURL, BuilderType.OPTIMISTIC_WORKLIST, null, false, DefaultSourceExtractor::new)
        .fst;
  }
}
