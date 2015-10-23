package com.ibm.wala.core.tests.arraybounds;

import java.io.IOException;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.CoreMatchers.*;

import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis;
import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis.UnnecessaryCheck;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.AllDueToBranchePiPolicy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

/**
 * The test data should be grouped, according to the behavior of the analysis.
 * All array accesses of a class are to be detected as "in bound" or all are to
 * be detected as "not in bound".
 * 
 * This test will only check if all found accesses behave accordingly and if the
 * number of array accesses is as expected.
 * 
 * So there is no explicit check for specific lines.
 * 
 * @author Stephan Gocht <stephan@gobro.de>
 *
 */
public class ArrayboundsAnalysisTest {
  private static ClassLoader CLASS_LOADER = ArrayboundsAnalysisTest.class.getClassLoader();

  private static final String DETECTABLE_TESTDATA = "Larraybounds/Detectable";
  private static final int DETECTABLE_NUMBER_OF_ARRAY_ACCESS = 21;

  private static final String NOT_DETECTABLE_TESTDATA = "Larraybounds/NotDetectable";
  private static final int NOT_DETECTABLE_NUMBER_OF_ARRAY_ACCESS = 7;

  private static final String NOT_IN_BOUND_TESTDATA = "Larraybounds/NotInBound";
  private static final int NOT_IN_BOUND_TESTDATA_NUMBER_OF_ARRAY_ACCESS = 4;

  private static IRFactory<IMethod> irFactory;
  private static AnalysisOptions options;
  private static AnalysisScope scope;
  private static ClassHierarchy cha;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @BeforeClass
  public static void init() throws IOException, ClassHierarchyException {
    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, null, CLASS_LOADER);
    cha = ClassHierarchy.make(scope);

    irFactory = new DefaultIRFactory();
    options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(new AllDueToBranchePiPolicy());
  }

  public static IR getIr(IMethod method) {
    return irFactory.makeIR(method, Everywhere.EVERYWHERE, options.getSSAOptions());
  }

  public static IClass getIClass(String name) {
    final TypeReference typeRef = TypeReference.findOrCreate(scope.getApplicationLoader(), name);
    return cha.lookupClass(typeRef);
  }

  @Test
  public void detectable() {
    IClass iClass = getIClass(DETECTABLE_TESTDATA);
    assertAllSameNecessity(iClass, DETECTABLE_NUMBER_OF_ARRAY_ACCESS, equalTo(UnnecessaryCheck.BOTH));
  }

  @Test
  public void notDetectable() {
    IClass iClass = getIClass(NOT_DETECTABLE_TESTDATA);
    assertAllSameNecessity(iClass, NOT_DETECTABLE_NUMBER_OF_ARRAY_ACCESS, not(equalTo(UnnecessaryCheck.BOTH)));
  }

  @Test
  public void notInBound() {
    IClass iClass = getIClass(NOT_IN_BOUND_TESTDATA);
    assertAllSameNecessity(iClass, NOT_IN_BOUND_TESTDATA_NUMBER_OF_ARRAY_ACCESS, not(equalTo(UnnecessaryCheck.BOTH)));
  }

  public void assertAllSameNecessity(IClass iClass, int expectedNumberOfArrayAccesses, Matcher<UnnecessaryCheck> matcher) {
    int numberOfArrayAccesses = 0;
    for (IMethod method : iClass.getAllMethods()) {
      if (method.getDeclaringClass().equals(iClass)) {
        String identifyer = method.getDeclaringClass().getName().toString() + "#" + method.getName().toString();

        ArrayOutOfBoundsAnalysis analysis = new ArrayOutOfBoundsAnalysis(getIr(method));
        for (UnnecessaryCheck unnecessary : analysis.getBoundsCheckNecessary().values()) {
          numberOfArrayAccesses++;
          collector.checkThat("Unexpected necessity for bounds check in " + identifyer, unnecessary, matcher);
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
    collector.checkThat("Number of found array accesses is not as expected for " + iClass.getName().toString(),
        numberOfArrayAccesses, equalTo(expectedNumberOfArrayAccesses));
  }

  @AfterClass
  public static void free() throws IOException, ClassHierarchyException {
    scope = null;
    cha = null;
    irFactory = null;
    options = null;
  }
}
