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
    assertThat(MethodTypeSignature.make("(I)V").getReturnType(), is(TypeSignature.make("V")));
  }

  @Test
  void arrayArgumentType() {
    assertThat(
        MethodTypeSignature.make("([I)V").getArguments(),
        arrayContaining(is(TypeSignature.make("[I"))));
    assertThat(
        MethodTypeSignature.make("([J[DB)V").getArguments(),
        arrayContaining(
            is(TypeSignature.make("[J")),
            is(TypeSignature.make("[D")),
            is(TypeSignature.make("B"))));
    assertThat(
        MethodTypeSignature.make("([Ljava/lang/String;B)V").getArguments(),
        arrayContaining(
            is(TypeSignature.make("[Ljava/lang/String;")), is(TypeSignature.make("B"))));
  }
}
