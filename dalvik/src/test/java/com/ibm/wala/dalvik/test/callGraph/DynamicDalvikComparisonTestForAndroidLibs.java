package com.ibm.wala.dalvik.test.callGraph;

import static com.ibm.wala.dalvik.test.util.Util.androidLibs;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrike.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class DynamicDalvikComparisonTestForAndroidLibs extends DynamicDalvikComparisonTest {

  protected URI[] providedAndroidLibs() {
    return androidLibs();
  }

  @Test
  public void testJLex()
      throws ClassHierarchyException,
          IllegalArgumentException,
          IOException,
          CancelException,
          InterruptedException,
          ClassNotFoundException,
          SecurityException,
          InvalidClassFileException,
          FailureException {
    File inputFile = testFile("sample.lex");
    test(
        providedAndroidLibs(),
        TestConstants.JLEX_MAIN,
        TestConstants.JLEX,
        inputFile.getAbsolutePath());
  }

  @Test
  public void testJavaCup()
      throws ClassHierarchyException,
          IllegalArgumentException,
          IOException,
          CancelException,
          InterruptedException,
          ClassNotFoundException,
          SecurityException,
          InvalidClassFileException,
          FailureException {
    File inputFile = testFile("sample.cup");
    test(
        providedAndroidLibs(),
        TestConstants.JAVA_CUP_MAIN,
        TestConstants.JAVA_CUP,
        inputFile.getAbsolutePath());
  }
}
