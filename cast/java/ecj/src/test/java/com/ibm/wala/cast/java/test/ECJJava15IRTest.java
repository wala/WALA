package com.ibm.wala.cast.java.test;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ibm.wala.util.CancelException;

public class ECJJava15IRTest extends ECJIRTests {

  public ECJJava15IRTest() {
    super(null);
    dump = true;
  }

  static Stream<String> java15IRTestNames() {
    return Stream.of(
        "AnonGeneNullarySimple",
        "AnonymousGenerics",
        "Annotations",
        "BasicsGenerics",
        "Cocovariant",
        "CustomGenericsAndFields",
        "EnumSwitch",
        "ExplicitBoxingTest",
        "GenericArrays",
        "GenericMemberClasses",
        "GenericSuperSink",
        "MoreOverriddenGenerics",
        "NotSoSimpleEnums",
        "OverridesOnePointFour",
        "SimpleEnums",
        "SimpleEnums2",
        "TypeInferencePrimAndStringOp",
        "Varargs",
        "VarargsCovariant",
        "VarargsOverriding",
        "Wildcards");
  }

  @ParameterizedTest
  @MethodSource("java15IRTestNames")
  public void runJava15IRTests(String java15IRTestName)
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointfive", java15IRTestName),
        rtJar,
        simplePkgTestEntryPoint("javaonepointfive", java15IRTestName),
        emptyList,
        true,
        null);
  }
}
