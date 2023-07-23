package com.ibm.wala.core.tests.typeargument;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.types.generics.TypeArgument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class TypeArgumentTest extends WalaTestCase {
  public static void main(String[] args) {
    justThisTest(TypeArgumentTest.class);
  }

  @AfterAll
  public static void afterClass() throws Exception {
    Warnings.clear();
  }

  @Test
  public void test() {
    // Test for StringIndexOutOfBoundsException
    TypeArgument.make("<Lorg/apache/hadoop/io/Text;[B>");
    // Test for "bad type argument list"
    TypeArgument.make("<[TP_OUT;>");
  }
}
