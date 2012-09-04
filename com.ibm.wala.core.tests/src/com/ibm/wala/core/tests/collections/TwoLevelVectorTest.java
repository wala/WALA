/*
 * Licensed Materials - Property of IBM
 * 5724-D15
 * (C) Copyright IBM Corporation 2008-2009. All Rights Reserved. 
 * Note to U.S. Government Users Restricted Rights:  
 * Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
 */
package com.ibm.wala.core.tests.collections;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.util.collections.TwoLevelVector;

/**
 * Tests {@link TwoLevelVector} class.
 * 
 * @author egeay
 */
public final class TwoLevelVectorTest extends WalaTestCase {
  
  public static void main(final String[] args) {
    justThisTest(TwoLevelVectorTest.class);
  }
  
  // --- Test cases
  
  @Test public void testCase1() {
    final TwoLevelVector<Integer> tlVector = new TwoLevelVector<Integer>();
    tlVector.iterator();
    tlVector.set(2147483647, 56);
    Assert.assertNotNull(tlVector.iterator());
    Assert.assertEquals(Integer.valueOf(56), tlVector.get(2147483647));
  }

}
