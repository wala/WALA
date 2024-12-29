package com.ibm.wala.types.generics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class TypeSignatureTest {

  @Test
  void basicTypeArguments() {
    assertThat(
        TypeSignature.parseForTypeSignatures("<Ljava/lang/String;ILjava/lang/Object;>"),
        arrayContaining(is("Ljava/lang/String;"), is("I"), is("Ljava/lang/Object;")));
  }

  @Test
  void multiDimArray() {
    assertThat(
        TypeSignature.parseForTypeSignatures("<[[Ljava/lang/String;[[[J>"),
        arrayContaining(is("[[Ljava/lang/String;"), is("[[[J")));
  }

}
