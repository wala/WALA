package com.ibm.wala.cast.js.test;

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

  private Map<String, String[]> getParsedJSONCG(CallGraph cg, CallGraph2JSON cg2JSON) {
    String json = cg2JSON.serialize(cg);
    System.err.println(json);
    Gson gson = new Gson();
    Type mapType = new TypeToken<Map<String, String[]>>() {}.getType();
    return gson.fromJson(json, mapType);
  }

  private CallGraph buildCallGraph(String script) throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    return util.buildCG(
            scriptURL, BuilderType.PESSIMISTIC, null, false, DefaultSourceExtractor::new)
        .fst;
  }
}
