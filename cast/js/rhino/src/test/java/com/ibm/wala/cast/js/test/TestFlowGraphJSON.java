package com.ibm.wala.cast.js.test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertArrayEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.cast.js.callgraph.fieldbased.FieldBasedCallGraphBuilder.CallGraphResult;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
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
    assertArrayEquals(
        new String[] {
          "Var(flowgraph_constraints.js@2, [f1])", "Var(flowgraph_constraints.js@1, %ssa_val 28)"
        },
        parsedJSON.get("Func(flowgraph_constraints.js@2)"));
  }

  @Test
  public void testParamAndReturn() {
    assertArrayEquals(
        new String[] {"Var(flowgraph_constraints.js@8, [p])"},
        parsedJSON.get("Param(Func(flowgraph_constraints.js@8), 2)"));
    assertArrayEquals(
        new String[] {"Ret(Func(flowgraph_constraints.js@8))"},
        parsedJSON.get("Var(flowgraph_constraints.js@8, [p])"));
    assertThat(
        Arrays.asList(parsedJSON.get("Var(flowgraph_constraints.js@12, [x])")),
        containsInAnyOrder(
            "Param(Func(flowgraph_constraints.js@8), 2)",
            "Args(Func(flowgraph_constraints.js@8))"));
    assertThat(
        Arrays.asList(parsedJSON.get("Var(flowgraph_constraints.js@17, [x])")),
        containsInAnyOrder(
            "Param(Func(flowgraph_constraints.js@8), 2)",
            "Args(Func(flowgraph_constraints.js@8))"));
    assertThat(
        Arrays.asList(parsedJSON.get("Ret(Func(flowgraph_constraints.js@8))")),
        containsInAnyOrder(
            "Var(flowgraph_constraints.js@17, [y])", "Var(flowgraph_constraints.js@12, [y])"));
  }

  @Test
  public void testCallAndApply() {
    assertThat(
        Arrays.asList(
            parsedJSON.get("Var(flowgraph_constraints.js@29, [nested, x, $$destructure$rcvr7])")),
        hasItems(
            "ReflectiveCallee(flowgraph_constraints.js@33)",
            "ReflectiveCallee(flowgraph_constraints.js@32)",
            "Param(Func(flowgraph_constraints.js@30), 2)"));
    assertThat(
        Arrays.asList(parsedJSON.get("Ret(Func(flowgraph_constraints.js@30))")),
        containsInAnyOrder(
            "Var(flowgraph_constraints.js@29, [res1])",
            "Var(flowgraph_constraints.js@29, [res2])"));
  }

  private static Map<String, String[]> getParsedFlowGraphJSON(String script)
      throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    FieldBasedCGUtil util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
    CallGraphResult callGraphResult =
        util.buildCG(
            scriptURL, BuilderType.OPTIMISTIC_WORKLIST, null, false, DefaultSourceExtractor::new);
    FlowGraph fg = callGraphResult.getFlowGraph();
    String json = fg.toJSON();
    // Strip out character offsets, as they differ on Windows and make it hard to write assertions.
    json = json.replaceAll(":[0-9]+-[0-9]+", "");
    //    System.err.println("CALL GRAPH:");
    //    System.err.println(callGraphResult.getCallGraph());
    //    System.err.println(json);
    Gson gson = new Gson();
    Type mapType = new TypeToken<Map<String, String[]>>() {}.getType();
    return gson.fromJson(json, mapType);
  }
}
