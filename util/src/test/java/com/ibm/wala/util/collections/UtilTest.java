package com.ibm.wala.util.collections;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated("objectFieldsToString temporarily installs a security manager")
class UtilTest {

  @Test
  void removePackageNamePreservesNull() {
    assertThat(Util.removePackageName(null)).isNull();
  }

  @Test
  void removePackageNameReturnsSimpleNameForQualifiedName() {
    assertThat(Util.removePackageName("java.lang.String")).isEqualTo("String");
  }

  @Test
  void removePackageNameReturnsEmptyStringForUnqualifiedName() {
    assertThat(Util.removePackageName("String")).isEmpty();
  }

  @Test
  @EnabledForJreRange(max = JRE.JAVA_17)
  void objectFieldsToStringIncludesNullFieldsAndSuperclassFields() {
    assertThat(Util.objectFieldsToString(new Child()))
        .isEqualTo("UtilTest$Child (label=null : String) (count=7 : )");
  }

  @Test
  @EnabledForJreRange(max = JRE.JAVA_17)
  void objectFieldsToStringStopsAtObjectWithoutInspectingItsFields() {
    assertThat(Util.objectFieldsToString(new Object())).isEqualTo("Object");
  }

  static class Parent {
    // An inherited field makes the reflection test exercise superclass traversal.
    public int count = 7;
  }

  static class Child extends Parent {
    // An unset field makes the reflection test exercise rendering a null value.
    public @Nullable String label;
  }
}
