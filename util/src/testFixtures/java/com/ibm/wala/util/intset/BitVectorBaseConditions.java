/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.intset;

import org.assertj.core.api.Condition;
import org.assertj.core.condition.VerboseCondition;

public class BitVectorBaseConditions {

  public static <T extends BitVectorBase<T>> Condition<T> bitSet(int bit) {
    return new Condition<>(actual -> actual.get(bit), "bit %s set", bit);
  }

  public static <T extends BitVectorBase<T>> Condition<T> emptyIntersectionWith(T other) {
    return new Condition<>(
        actual -> actual.intersectionEmpty(other), "empty intersection with %s", other);
  }

  public static <T extends BitVectorBase<T>> Condition<T> max(int expectedMax) {
    return VerboseCondition.verboseCondition(
        actual -> actual.max() == expectedMax,
        "max bit %s".formatted(expectedMax),
        actual -> " instead of %s".formatted(actual.max()));
  }

  public static <T extends BitVectorBase<T>> Condition<T> sameBitsAs(T other) {
    return new Condition<>(actual -> actual.sameBits(other), "same bits as %s", other);
  }

  public static <T extends BitVectorBase<T>> Condition<T> subsetOf(T other) {
    return new Condition<>(actual -> actual.isSubset(other), "subset of %s", other);
  }
}
