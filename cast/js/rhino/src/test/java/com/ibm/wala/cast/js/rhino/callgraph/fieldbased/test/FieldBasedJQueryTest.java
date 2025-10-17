package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.util.FieldBasedCGUtil.BuilderType;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("requires-Internet")
public class FieldBasedJQueryTest extends AbstractFieldBasedTest {

  @Test
  public void test1_8_2()
      throws IOException, WalaException, Error, CancelException, URISyntaxException {
    runTest(
        new URI("http://code.jquery.com/jquery-1.8.2.js").toURL(),
        List.of(),
        BuilderType.OPTIMISTIC_WORKLIST);
  }
}
