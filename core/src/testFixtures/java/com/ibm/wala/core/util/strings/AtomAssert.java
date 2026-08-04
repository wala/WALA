package com.ibm.wala.core.util.strings;

import java.io.UTFDataFormatException;
import java.util.Arrays;
import java.util.Objects;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractObjectAssert;

/**
 * Custom {@link org.assertj.core.api.Assertions} assertions for {@link Atom} instances, for use in
 * WALA's own tests.
 */
public class AtomAssert extends AbstractObjectAssert<AtomAssert, Atom> {

  public AtomAssert(final Atom actual) {
    super(actual, AtomAssert.class);
  }

  @CheckReturnValue
  public static AtomAssert assertThat(Atom actual) {
    return new AtomAssert(actual);
  }

  public AtomAssert hasUnicodeString(String expected) {
    isNotNull();
    final String actualUnicode;
    try {
      actualUnicode = actual.toUnicodeString();
    } catch (UTFDataFormatException problem) {
      failWithMessage(
          "\nExpecting atom `%s` to decode to `%s` but its bytes are not valid UTF-8.",
          actual, expected);
      return this;
    }
    if (!Objects.equals(actualUnicode, expected)) {
      failWithMessage(
          "\nExpecting atom to decode to `%s` but decoded to `%s`.", expected, actualUnicode);
    }
    return this;
  }

  public AtomAssert isInternedAs(Atom expected) {
    isNotNull();
    if (actual != expected) {
      failWithMessage(
          "\nExpecting atom `%s` to be the canonical instance for `%s`, but it is not.",
          actual, expected);
    }
    return this;
  }

  public AtomAssert isNotInternedAs(Atom expected) {
    isNotNull();
    if (actual == expected) {
      failWithMessage(
          "\nExpecting atom `%s` not to be the canonical instance for `%s`, but it is.",
          actual, expected);
    }
    return this;
  }

  public AtomAssert hasByteLength(int expected) {
    isNotNull();
    if (actual.length() != expected) {
      failWithMessage(
          "\nExpecting atom to have byte length %d but has %d.", expected, actual.length());
    }
    return this;
  }

  public AtomAssert isEmpty() {
    isNotNull();
    if (actual.length() != 0) {
      failWithMessage("\nExpecting atom to be empty but has byte length %d.", actual.length());
    }
    return this;
  }

  public AtomAssert hasValArray(byte... expected) {
    isNotNull();
    if (!Arrays.equals(actual.getValArray(), expected)) {
      failWithMessage(
          "\nExpecting atom to have bytes %s but has %s.",
          Arrays.toString(expected), Arrays.toString(actual.getValArray()));
    }
    return this;
  }

  public AtomAssert hasByteAt(int index, byte expected) {
    isNotNull();
    if (actual.getVal(index) != expected) {
      failWithMessage(
          "\nExpecting byte at index %d to be %d but is %d.",
          index, expected, actual.getVal(index));
    }
    return this;
  }

  public AtomAssert contains(byte b) {
    isNotNull();
    if (!actual.contains(b)) {
      failWithMessage("\nExpecting atom `%s` to contain byte %d but it does not.", actual, b);
    }
    return this;
  }

  public AtomAssert doesNotContain(byte b) {
    isNotNull();
    if (actual.contains(b)) {
      failWithMessage("\nExpecting atom `%s` not to contain byte %d but it does.", actual, b);
    }
    return this;
  }

  public AtomAssert hasRIndex(byte b, int expected) {
    isNotNull();
    if (actual.rIndex(b) != expected) {
      failWithMessage(
          "\nExpecting right index of byte %d to be %d but is %d.", b, expected, actual.rIndex(b));
    }
    return this;
  }

  public AtomAssert startsWith(Atom expected) {
    isNotNull();
    if (!actual.startsWith(expected)) {
      failWithMessage(
          "\nExpecting atom `%s` to start with `%s` but it does not.", actual, expected);
    }
    return this;
  }

  public AtomAssert doesNotStartWith(Atom expected) {
    isNotNull();
    if (actual.startsWith(expected)) {
      failWithMessage(
          "\nExpecting atom `%s` not to start with `%s` but it does.", actual, expected);
    }
    return this;
  }

  public AtomAssert isReservedMemberName() {
    isNotNull();
    if (!actual.isReservedMemberName()) {
      failWithMessage("\nExpecting atom `%s` to be a reserved member name but it is not.", actual);
    }
    return this;
  }

  public AtomAssert isNotReservedMemberName() {
    isNotNull();
    if (actual.isReservedMemberName()) {
      failWithMessage("\nExpecting atom `%s` not to be a reserved member name but it is.", actual);
    }
    return this;
  }

  public AtomAssert isClassDescriptor() {
    isNotNull();
    if (!actual.isClassDescriptor()) {
      failWithMessage("\nExpecting atom `%s` to be a class descriptor but it is not.", actual);
    }
    return this;
  }

  public AtomAssert isNotClassDescriptor() {
    isNotNull();
    if (actual.isClassDescriptor()) {
      failWithMessage("\nExpecting atom `%s` not to be a class descriptor but it is.", actual);
    }
    return this;
  }

  public AtomAssert isArrayDescriptor() {
    isNotNull();
    if (!actual.isArrayDescriptor()) {
      failWithMessage("\nExpecting atom `%s` to be an array descriptor but it is not.", actual);
    }
    return this;
  }

  public AtomAssert isNotArrayDescriptor() {
    isNotNull();
    if (actual.isArrayDescriptor()) {
      failWithMessage("\nExpecting atom `%s` not to be an array descriptor but it is.", actual);
    }
    return this;
  }

  public AtomAssert hasArrayDimensionality(int expected) {
    isNotNull();
    if (actual.parseForArrayDimensionality() != expected) {
      failWithMessage(
          "\nExpecting atom `%s` to have array dimensionality %d but has %d.",
          actual, expected, actual.parseForArrayDimensionality());
    }
    return this;
  }

  public AtomAssert isMethodDescriptor() {
    isNotNull();
    if (!actual.isMethodDescriptor()) {
      failWithMessage("\nExpecting atom `%s` to be a method descriptor but it is not.", actual);
    }
    return this;
  }

  public AtomAssert isNotMethodDescriptor() {
    isNotNull();
    if (actual.isMethodDescriptor()) {
      failWithMessage("\nExpecting atom `%s` not to be a method descriptor but it is.", actual);
    }
    return this;
  }
}
