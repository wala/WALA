package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class TestFlowGraphJSON {

  private FieldBasedCGUtil util;

  @Before
  public void setUp() throws Exception {
    util = new FieldBasedCGUtil(new CAstRhinoTranslatorFactory());
  }

  @Test
  public void testBasic() throws WalaException, CancelException {
    String script = "tests/fieldbased/simple.js";
    FlowGraph fg = buildFlowGraph(script);
    String json = fg.toJSON();
    System.err.println(json);
  }

  private FlowGraph buildFlowGraph(String script) throws WalaException, CancelException {
    URL scriptURL = TestCallGraph2JSON.class.getClassLoader().getResource(script);
    return util.buildCG(
            scriptURL, BuilderType.OPTIMISTIC_WORKLIST, null, false, DefaultSourceExtractor::new)
        .getFlowGraph();
  }
}
