package com.ibm.wala.classLoader;

import org.assertj.core.api.Condition;

public class ShrikeClassCondition extends Condition<ShrikeClass> {

  public static final Condition<ShrikeClass> innerClass =
      new ShrikeClassCondition(ShrikeClass::isInnerClass, "inner class");

  public static final Condition<ShrikeClass> staticInnerClass =
      new ShrikeClassCondition(ShrikeClass::isStaticInnerClass, "static inner class");

  ShrikeClassCondition(final ThrowingPredicate<ShrikeClass> predicate, final String description) {
    super(predicate, description);
  }
}
