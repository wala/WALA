package com.ibm.wala.classLoader;

import java.util.function.Predicate;

interface ThrowingPredicate<T> extends Predicate<T> {

  boolean tryTest(T t) throws Exception;

  @Override
  default boolean test(T value) {
    try {
      return tryTest(value);
    } catch (final Exception problem) {
      throw new RuntimeException(problem);
    }
  }
}
