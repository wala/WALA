package com.ibm.wala.dalvik.test.ir;

import static com.ibm.wala.dalvik.test.DalvikTestBase.convertJarToDex;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;

import com.ibm.wala.core.tests.ir.AnnotationTest;
import com.ibm.wala.dalvik.test.DalvikTestBase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.TemporaryFile;

public class DalvikAnnotationsTest extends AnnotationTest {

  public static void main(String[] args) {
    justThisTest(DalvikAnnotationsTest.class);
  }
  
  private static IClassHierarchy makeCHA() throws IOException, ClassHierarchyException {
    File F = File.createTempFile("walatest", ".jar");
    F.deleteOnExit();
    TemporaryFile.urlToFile(F, (new FileProvider()).getResource("com.ibm.wala.core.testdata_1.0.0a.jar"));
    File androidDex = convertJarToDex(F.getAbsolutePath());
    AnalysisScope dalvikScope = DalvikTestBase.makeDalvikScope(null, null, androidDex.getAbsolutePath());
    return ClassHierarchy.make(dalvikScope);    
  }
  
  public DalvikAnnotationsTest() throws ClassHierarchyException, IOException {
    super(makeCHA());
  }
  
  @Override
  protected void assertEquals(Object a, Object b) {
    Assert.assertEquals(a, b);
  }

  @Override
  protected void assertNotNull(String msg, Object obj) {
    Assert.assertNotNull(msg, obj);
  }

  @Override
  protected void assertTrue(String x, boolean b) {
    Assert.assertTrue(x, b);
  }

}
