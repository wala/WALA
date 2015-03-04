package com.ibm.wala.core.tests.ir;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

public class JVMLAnnotationTest extends AnnotationTest {

  public static void main(String[] args) {
    justThisTest(JVMLAnnotationTest.class);
  }
  
  @BeforeClass
  public static void before() throws IOException, ClassHierarchyException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS), AnnotationTest.class.getClassLoader());
    cha = ClassHierarchy.make(scope);    
  }
  
  @AfterClass
  public static void after() {
    cha = null; 
  }

}
