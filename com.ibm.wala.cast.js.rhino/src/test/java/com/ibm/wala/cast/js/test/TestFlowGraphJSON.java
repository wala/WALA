package com.ibm.wala.cast.js.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.WalaException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestFlowGraphJSON {

  private static final String SCRIPT = "tests/fieldbased/flowgraph_constraints.js";

  private Map<String, String[]> parsedJSON;

  @Before
  public void setUp() throws Exception {
    parsedJSON = getParsedFlowGraphJSON(SCRIPT);
  }

  @Test
  public void testNamedIIFE() {
    boolean onWindows = PlatformUtil.onWindows();
    String[] targets =
        onWindows
            ? parsedJSON.get("Func(flowgraph_constraints.js@2:26-42)")
            : parsedJSON.get("Func(flowgraph_constraints.js@2:25-41)");
    Assert.assertNotNull(
        parsedJSON.keySet().stream()
            .filter(s -> s.startsWith("Func(flowgraph_constraints.js@2"))
            .collect(Collectors.toList())
            .toString(),
        targets);
    String[] expected =
        onWindows
            ? new String[] {
              "Var(flowgraph_constraints.js@2:26-42, [f1])",
              "Var(flowgraph_constraints.js@1:1-90, %ssa_val 3)"
            }
            : new String[] {
              "Var(flowgraph_constraints.js@2:25-41, [f1])",
              "Var(flowgraph_constraints.js@1:0-89, %ssa_val 3)"
            };
    Assert.assertArrayEquals(expected, targets);
  }

  private static Map<String, String[]> getParsedFlowGraphJSON(String script)
      throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    FieldBasedCGUtil util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
    FlowGraph fg =
        util.buildCG(
                scriptURL,
                BuilderType.OPTIMISTIC_WORKLIST,
                null,
                false,
                DefaultSourceExtractor::new)
            .getFlowGraph();
    String json = fg.toJSON();
    System.err.println(json);
    Gson gson = new Gson();
    Type mapType = new TypeToken<Map<String, String[]>>() {}.getType();
    return gson.fromJson(json, mapType);
  }
}
