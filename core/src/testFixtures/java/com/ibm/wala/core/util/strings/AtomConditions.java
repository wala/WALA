package com.ibm.wala.core.util.strings;

import org.assertj.core.api.Condition;

/** Custom {@link Condition} instances for {@link Atom} descriptors and reserved names. */
@SuppressWarnings("deprecation")
public final class AtomConditions {

  public static final Condition<Atom> reservedMemberName =
      new Condition<>(Atom::isReservedMemberName, "reserved member name");

  public static final Condition<Atom> classDescriptor =
      new Condition<>(Atom::isClassDescriptor, "class descriptor");

  public static final Condition<Atom> arrayDescriptor =
      new Condition<>(Atom::isArrayDescriptor, "array descriptor");

  public static final Condition<Atom> methodDescriptor =
      new Condition<>(Atom::isMethodDescriptor, "method descriptor");

  private AtomConditions() {}
}
