package com.ibm.wala.core.tests.arraybounds;

import static com.ibm.wala.core.tests.arraybounds.EqualTo.equalTo;
import static org.assertj.core.api.Assertions.not;

import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis;
import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis.UnnecessaryCheck;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.AllIntegerDueToBranchePiPolicy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import java.io.IOException;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The test data should be grouped, according to the behavior of the analysis. All array accesses of
 * a class are to be detected as "in bound" or all are to be detected as "not in bound".
 *
 * <p>This test will only check if all found accesses behave accordingly and if the number of array
 * accesses is as expected.
 *
 * <p>So there is no explicit check for specific lines.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
@ExtendWith(SoftAssertionsExtension.class)
public class ArrayboundsAnalysisTest {
  private static final ClassLoader CLASS_LOADER = ArrayboundsAnalysisTest.class.getClassLoader();

  private static final String DETECTABLE_TESTDATA = "Larraybounds/Detectable";
  private static final int DETECTABLE_NUMBER_OF_ARRAY_ACCESS = 34;

  private static final String NOT_DETECTABLE_TESTDATA = "Larraybounds/NotDetectable";
  private static final int NOT_DETECTABLE_NUMBER_OF_ARRAY_ACCESS = 10;

  private static final String NOT_IN_BOUND_TESTDATA = "Larraybounds/NotInBound";
  private static final int NOT_IN_BOUND_TESTDATA_NUMBER_OF_ARRAY_ACCESS = 8;

  private static IRFactory<IMethod> irFactory;
  private static AnalysisOptions options;
  private static AnalysisScope scope;
  private static ClassHierarchy cha;

  @BeforeAll
  public static void init() throws IOException, ClassHierarchyException {
    scope =
        AnalysisScopeReader.instance.readJavaScope(TestConstants.WALA_TESTDATA, null, CLASS_LOADER);
    cha = ClassHierarchyFactory.make(scope);

    irFactory = new DefaultIRFactory();
    options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(new AllIntegerDueToBranchePiPolicy());
  }

  public static IR getIr(IMethod method) {
    return irFactory.makeIR(method, Everywhere.EVERYWHERE, options.getSSAOptions());
  }

  public static IClass getIClass(String name) {
    final TypeReference typeRef = TypeReference.findOrCreate(scope.getApplicationLoader(), name);
    return cha.lookupClass(typeRef);
  }

  @Test
  public void detectable(final SoftAssertions softly) {
    IClass iClass = getIClass(DETECTABLE_TESTDATA);
    assertAllSameNecessity(
        softly, iClass, DETECTABLE_NUMBER_OF_ARRAY_ACCESS, equalTo(UnnecessaryCheck.BOTH));
  }

  @Test
  public void notDetectable(final SoftAssertions softly) {
    IClass iClass = getIClass(NOT_DETECTABLE_TESTDATA);
    assertAllSameNecessity(
        softly, iClass, NOT_DETECTABLE_NUMBER_OF_ARRAY_ACCESS, not(equalTo(UnnecessaryCheck.BOTH)));
  }

  @Test
  public void notInBound(final SoftAssertions softly) {
    IClass iClass = getIClass(NOT_IN_BOUND_TESTDATA);
    assertAllSameNecessity(
        softly,
        iClass,
        NOT_IN_BOUND_TESTDATA_NUMBER_OF_ARRAY_ACCESS,
        not(equalTo(UnnecessaryCheck.BOTH)));
  }

  public void assertAllSameNecessity(
      final SoftAssertions softly,
      IClass iClass,
      int expectedNumberOfArrayAccesses,
      Condition<UnnecessaryCheck> condition) {
    int numberOfArrayAccesses = 0;
    for (IMethod method : iClass.getAllMethods()) {
      if (method.getDeclaringClass().equals(iClass)) {
        IR ir = getIr(method);
        StringBuilder builder = new StringBuilder();
        for (ISSABasicBlock block : ir.getControlFlowGraph()) {
          for (SSAInstruction instruction : block) {
            builder.append(instruction);
            builder.append('\n');
          }
        }

        String identifier =
            method.getDeclaringClass().getName().toString() + "#" + method.getName().toString();

        ArrayOutOfBoundsAnalysis analysis = new ArrayOutOfBoundsAnalysis(ir);
        for (SSAArrayReferenceInstruction key : analysis.getBoundsCheckNecessary().keySet()) {
          numberOfArrayAccesses++;
          UnnecessaryCheck unnecessary = analysis.getBoundsCheckNecessary().get(key);
          softly
              .assertThat(unnecessary)
              .as(
                  () ->
                      "Unexpected necessity for bounds check in "
                          + identifier
                          + ":"
                          + method.getLineNumber(key.iIndex()))
              .satisfies(condition);
        }
      }
    }

    /*
     * Possible reasons for this to fail are:
     *
     *
     * *_NUMBER_OF_ARRAY_ACCESS is not set to the correct value (maybe the test
     * data has changed).
     *
     * Not all methods of the class analyzed.
     *
     * There is a bug, so not all array accesses are found.
     */
    softly
        .assertThat(numberOfArrayAccesses)
        .as(
            () ->
                "Number of found array accesses is not as expected for "
                    + iClass.getName().toString())
        .isEqualTo(expectedNumberOfArrayAccesses);
  }

  @AfterAll
  public static void free() {
    scope = null;
    cha = null;
    irFactory = null;
    options = null;
  }
}
