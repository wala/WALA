package com.ibm.wala.ipa.cha;

import com.ibm.wala.classLoader.IClass;
import org.assertj.core.api.Condition;

public class IClassHierarchyConditions {

  public static Condition<IClassHierarchy> implementsInterface(IClass c, IClass i) {
    return new Condition<>(
        actual -> actual.implementsInterface(c, i), "%s implements interface %s", c, i);
  }

  public static Condition<IClassHierarchy> isAssignableFrom(IClass c1, IClass c2) {
    return new Condition<>(
        actual -> actual.isAssignableFrom(c1, c2), "%s is assignable from %s", c1, c2);
  }

  public static Condition<IClassHierarchy> subclassOf(IClass subClass, IClass superClass) {
    return new Condition<>(
        actual -> actual.isSubclassOf(subClass, superClass),
        "%s is subclass of %s",
        subClass,
        superClass);
  }
}
