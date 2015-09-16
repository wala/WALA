package com.ibm.wala.dalvik.test.ir;

import java.io.IOException;

import com.ibm.wala.core.tests.ir.AnnotationTest;
import com.ibm.wala.core.tests.util.JVMLTestAssertions;
import com.ibm.wala.dalvik.test.util.Util;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class DalvikAnnotationsTest extends AnnotationTest {

  public static void main(String[] args) {
    justThisTest(DalvikAnnotationsTest.class);
  }
    
  public DalvikAnnotationsTest() throws ClassHierarchyException, IOException {
    super(new JVMLTestAssertions(), Util.makeCHA());
  }
  

}
