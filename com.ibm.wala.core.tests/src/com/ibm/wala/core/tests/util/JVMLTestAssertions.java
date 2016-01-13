package com.ibm.wala.core.tests.util;

import org.junit.Assert;

public class JVMLTestAssertions extends TestAssertions {

  @Override
  public void assertEquals(Object a, Object b) {
    Assert.assertEquals(a, b);
  }

  @Override
  public void assertNotNull(String msg, Object obj) {
    Assert.assertNotNull(msg, obj);
  }

  @Override
  public void assertTrue(String x, boolean b) {
    Assert.assertTrue(x, b);
  }

}
