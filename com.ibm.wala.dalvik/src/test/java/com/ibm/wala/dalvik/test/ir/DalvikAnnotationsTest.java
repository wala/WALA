package com.ibm.wala.dalvik.test.ir;

import com.ibm.wala.core.tests.ir.AnnotationTest;
import com.ibm.wala.dalvik.test.util.Util;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import java.io.IOException;

public class DalvikAnnotationsTest extends AnnotationTest {

  public static void main(String[] args) {
    justThisTest(DalvikAnnotationsTest.class);
  }

  public DalvikAnnotationsTest() throws ClassHierarchyException, IOException {
    super(Util.makeCHA(), true);
  }
}
