package com.ibm.wala.dalvik.test.callGraph;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrike.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DynamicDalvikComparisonJavaLibsTest extends DynamicDalvikComparisonTest {

  private @TempDir Path temporaryDirectory;

  @Override
  protected Path getTemporaryDirectory() {
    return temporaryDirectory;
  }

  @Test
  public void testJLex()
      throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException,
          InterruptedException, ClassNotFoundException, SecurityException,
          InvalidClassFileException, FailureException {
    File inputFile = testFile("sample.lex");
    test(null, TestConstants.JLEX_MAIN, TestConstants.JLEX, inputFile.getAbsolutePath());
  }

  @Test
  public void testJavaCup()
      throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException,
          InterruptedException, ClassNotFoundException, SecurityException,
          InvalidClassFileException, FailureException {
    File inputFile = testFile("sample.cup");
    test(null, TestConstants.JAVA_CUP_MAIN, TestConstants.JAVA_CUP, inputFile.getAbsolutePath());
  }
}
