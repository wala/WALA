package com.ibm.wala.dalvik.test.callGraph;

import java.io.File;
import java.io.IOException;
import org.junit.Test;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

public class DynamicDalvikComparisonJavaLibsTest extends DynamicDalvikComparisonTest {

  @Test
  public void testJLex() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, InvalidClassFileException, FailureException {
    File inputFile = testFile("sample.lex");
    test(null, TestConstants.JLEX_MAIN, TestConstants.JLEX, inputFile.getAbsolutePath());
  }

  @Test
  public void testJavaCup() throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, InterruptedException, ClassNotFoundException, SecurityException, InvalidClassFileException, FailureException {
    File inputFile = testFile("sample.cup");
    test(null, TestConstants.JAVA_CUP_MAIN, TestConstants.JAVA_CUP, inputFile.getAbsolutePath());
  }

}
