package com.ibm.wala.dalvik.test.callGraph.droidbench;

import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FieldAndObjectSensitivityTest extends DroidBenchCGTest {

  @MethodSource("generateData")
  @Override
  @ParameterizedTest
  protected void runTest(final TestParameters testParameters) throws Exception {
    super.runTest(testParameters);
  }

  static Stream<Named<TestParameters>> generateData() {
    return generateData("FieldAndObjectSensitivity");
  }
}
