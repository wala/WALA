package com.ibm.wala.core.tests.typeargument;

import org.junit.AfterClass;
import org.junit.Test;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.types.generics.TypeArgument;
import com.ibm.wala.util.warnings.Warnings;

public class TypeArgumentTest extends WalaTestCase {
    public static void main(String[] args) {
      justThisTest(TypeArgumentTest.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
      Warnings.clear();
    }
    @Test public void test() {
      //Test for StringIndexOutOfBoundsException
      TypeArgument.make("<Lorg/apache/hadoop/io/Text;[B>");
      //Test for "bad type argument list"
      TypeArgument.make("<[TP_OUT;>");
    }
}
