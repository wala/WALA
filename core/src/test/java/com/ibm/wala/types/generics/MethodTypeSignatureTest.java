package com.ibm.wala.types.generics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link MethodTypeSignature}. */
class MethodTypeSignatureTest {

  /** Unit test for {@link MethodTypeSignature#getArguments()}. */
  @Test
  void getArguments() {
    assertThat(
        MethodTypeSignature.make("(I)V").getArguments(),
        arrayContaining(is(TypeSignature.make("I"))));
  }

  @Test
  void getVoidReturn() {
    assertThat(
        MethodTypeSignature.make("(I)V").getReturnType(),
        is(TypeSignature.make("V")));
  }
}
