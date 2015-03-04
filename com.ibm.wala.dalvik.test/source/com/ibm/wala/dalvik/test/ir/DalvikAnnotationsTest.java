package com.ibm.wala.dalvik.test.ir;

import static com.ibm.wala.dalvik.test.DalvikTestBase.convertJarToDex;
import static com.ibm.wala.dalvik.test.DalvikTestBase.makeDalvikScope;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.wala.core.tests.ir.AnnotationTest;
import com.ibm.wala.core.tests.ir.JVMLAnnotationTest;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.io.TemporaryFile;

public class DalvikAnnotationsTest extends AnnotationTest {

  public static void main(String[] args) {
    justThisTest(JVMLAnnotationTest.class);
  }
  
  @BeforeClass
  public static void before() throws IOException, ClassHierarchyException {
    File F = File.createTempFile("waladata", ".jar");
    F.deleteOnExit();
    TemporaryFile.streamToFile(F, DalvikAnnotationsTest.class.getClassLoader().getResourceAsStream("com.ibm.wala.core.testdata_1.0.0a.jar"));
    File androidDex = convertJarToDex(F.getAbsolutePath());
    AnalysisScope dalvikScope = makeDalvikScope(true, androidDex.getAbsolutePath());
    cha = ClassHierarchy.make(dalvikScope);    
  }
  
  @AfterClass
  public static void after() {
    cha = null; 
  }

}
