package com.ibm.wala.cast.java.test;

import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ECJJava15IRTest extends ECJIRTests {

  public ECJJava15IRTest() {
    super(null);
    dump = true;
  }


  @Override
  protected AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, ?> getAnalysisEngine(
      final String[] mainClassDescriptors, Collection<Path> sources, List<String> libs) {
    JavaSourceAnalysisEngine engine =
        new ECJJavaSourceAnalysisEngine() {
          @Override
          protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
            return Util.makeMainEntrypoints(
                JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
          }
        };
    engine.setExclusionsFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    populateScope(engine, sources, libs);
    return engine;
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
