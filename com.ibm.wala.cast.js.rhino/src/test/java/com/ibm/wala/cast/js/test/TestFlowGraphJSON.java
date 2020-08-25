package com.ibm.wala.cast.js.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.lang.reflect.Type;
import java.net.URL;
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
  public void testBasic() throws WalaException, CancelException {}

  //  @Test
  //  public void testBasic2() throws WalaException, CancelException {
  //  }

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
