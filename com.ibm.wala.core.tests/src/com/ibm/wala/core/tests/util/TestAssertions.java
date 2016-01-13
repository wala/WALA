package com.ibm.wala.core.tests.util;

import java.util.Collection;
import java.util.Collections;


public abstract class TestAssertions {

  public abstract void assertEquals(Object findOrCreate, Object type);

  public abstract void assertNotNull(String string, Object classUnderTest);

  public abstract void assertTrue(String x, boolean b);

  public <T> void assertEqualCollections(Collection<T> expected, Collection<T> actual) {
    if (expected == null) {
      expected = Collections.emptySet();
    }
    if (actual == null) {
      actual = Collections.emptySet();
    }

    if (expected.size() != actual.size()) {
      assertTrue("expected=" + expected + " actual=" + actual, false);
    }
    for (T a : expected) {
      assertTrue("missing " + a.toString(), actual.contains(a));
    }
  }

  public void assertBound(String tag, double quantity, double bound) {
    String msg = tag + ", quantity: " + quantity + ", bound:" + bound;
    System.err.println(msg);
    assertTrue(msg, quantity <= bound);
  }

  public void assertBound(String tag, int quantity, int bound) {
    String msg = tag + ", quantity: " + quantity + ", bound:" + bound;
    System.err.println(msg);
    assertTrue(msg, quantity <= bound);
  }

}
