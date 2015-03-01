package com.ibm.wala.dalvik.test.callGraph.droidbench;

import java.util.Collection;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import com.ibm.wala.types.MethodReference;

	@RunWith(Parameterized.class)
  public class FieldAndObjectSensitivityTest extends DroidBenchCGTest {
    public FieldAndObjectSensitivityTest(String apkFile, Set<MethodReference> uncalled) {
      super(apkFile, uncalled);
    }

    @Parameters //(name="DroidBench: {0}")
    public static Collection<Object[]> generateData() {
      return DroidBenchCGTest.generateData("FieldAndObjectSensitivity");
    }
  }
