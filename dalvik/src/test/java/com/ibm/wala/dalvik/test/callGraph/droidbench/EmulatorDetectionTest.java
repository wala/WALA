package com.ibm.wala.dalvik.test.callGraph.droidbench;

import static com.ibm.wala.dalvik.test.util.Util.androidJavaLib;

import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EmulatorDetectionTest extends DroidBenchCGTest {

  @MethodSource("generateData")
  @Override
  @ParameterizedTest
  protected void runTest(final TestParameters testParameters) throws Exception {
    super.runTest(testParameters);
  }

  static Stream<Named<TestParameters>> generateData() throws IOException {
    return DroidBenchCGTest.generateData(null, androidJavaLib(), "EmulatorDetection");
  }
}
