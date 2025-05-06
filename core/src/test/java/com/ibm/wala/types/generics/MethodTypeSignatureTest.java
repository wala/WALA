package com.ibm.wala.types.generics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link MethodTypeSignature}. */
class MethodTypeSignatureTest {

  /** Unit test for {@link MethodTypeSignature#getArguments()}. */
  @Test
  void getArguments() {
    assertThat(MethodTypeSignature.make("(I)V").getArguments()).contains(TypeSignature.make("I"));
  }

  @Test
  void getVoidReturn() {
    assertThat(MethodTypeSignature.make("(I)V").getReturnType()).isEqualTo(TypeSignature.make("V"));
  }

  @Test
  void arrayArgumentType() {
    assertThat(MethodTypeSignature.make("([I)V").getArguments()).contains(TypeSignature.make("[I"));
    assertThat(MethodTypeSignature.make("([J[DB)V").getArguments())
        .contains(TypeSignature.make("[J"), TypeSignature.make("[D"), TypeSignature.make("B"));
    assertThat(MethodTypeSignature.make("([Ljava/lang/String;B)V").getArguments())
        .contains(TypeSignature.make("[Ljava/lang/String;"), TypeSignature.make("B"));
  }
}
