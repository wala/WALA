package com.ibm.wala.dalvik.test.callGraph.droidbench;

import static com.ibm.wala.dalvik.test.util.Util.androidJavaLib;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import com.ibm.wala.types.MethodReference;

@RunWith(Parameterized.class)
public class ReflectionTest extends DroidBenchCGTest {

  public ReflectionTest(URI[] androidLibs, File androidJavaJar, String apkFile, Set<MethodReference> uncalled) {
    super(androidLibs, androidJavaJar, apkFile, uncalled);
  }

  @Parameters(name="DroidBench: {2}")
  public static Collection<Object[]> generateData() throws IOException {
    return DroidBenchCGTest.generateData(null, androidJavaLib(), "Reflection");
  }
}
