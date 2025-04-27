package com.ibm.wala.core.tests.arraybounds;

import org.assertj.core.api.Condition;

class EqualTo {

  static <T> Condition<T> equalTo(final T expected) {
    return new Condition<>(actual -> actual.equals(expected), "equals %s", expected);
  }
}
