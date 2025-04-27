package com.ibm.wala.core.tests.exceptionpruning;

import com.ibm.wala.analysis.exceptionanalysis.ExceptionAnalysis;
import com.ibm.wala.analysis.exceptionanalysis.IntraproceduralExceptionAnalysis;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.ref.ReferenceCleanser;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.IgnoreExceptionsFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.CombinedInterproceduralExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.IgnoreExceptionsInterFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.AllIntegerDueToBranchePiPolicy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This class checks, if the number of exceptions which might occur intra and interprocedural is
 * right. As well as the number of caught exceptions for each call site.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
@ExtendWith(SoftAssertionsExtension.class)
public class ExceptionAnalysisTest {
  private static final ClassLoader CLASS_LOADER = ExceptionAnalysisTest.class.getClassLoader();
  public static String REGRESSION_EXCLUSIONS = "Java60RegressionExclusions.txt";

  private static ClassHierarchy cha;
  private static CallGraph cg;
  private static PointerAnalysis<InstanceKey> pointerAnalysis;
  private static CombinedInterproceduralExceptionFilter<SSAInstruction> filter;

  @BeforeAll
  public static void init()
      throws IOException,
          ClassHierarchyException,
          IllegalArgumentException,
          CallGraphBuilderCancelException {
    AnalysisOptions options;
    AnalysisScope scope;

    scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA, new File(REGRESSION_EXCLUSIONS), CLASS_LOADER);
    cha = ClassHierarchyFactory.make(scope);

    Iterable<Entrypoint> entrypoints =
        Util.makeMainEntrypoints(cha, "Lexceptionpruning/TestPruning");
    options = new AnalysisOptions(scope, entrypoints);
    options.getSSAOptions().setPiNodePolicy(new AllIntegerDueToBranchePiPolicy());

    ReferenceCleanser.registerClassHierarchy(cha);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    ReferenceCleanser.registerCache(cache);
    CallGraphBuilder<InstanceKey> builder =
        Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha);
    cg = builder.makeCallGraph(options, null);
    pointerAnalysis = builder.getPointerAnalysis();

    /*
     * We will ignore some exceptions to focus on the exceptions we want to
     * raise (OwnException, ArrayIndexOutOfBoundException)
     */
    filter = new CombinedInterproceduralExceptionFilter<>();
    filter.add(
        new IgnoreExceptionsInterFilter<>(
            new IgnoreExceptionsFilter(TypeReference.JavaLangOutOfMemoryError)));
    filter.add(
        new IgnoreExceptionsInterFilter<>(
            new IgnoreExceptionsFilter(TypeReference.JavaLangNullPointerException)));
    filter.add(
        new IgnoreExceptionsInterFilter<>(
            new IgnoreExceptionsFilter(TypeReference.JavaLangExceptionInInitializerError)));
    filter.add(
        new IgnoreExceptionsInterFilter<>(
            new IgnoreExceptionsFilter(TypeReference.JavaLangExceptionInInitializerError)));
    filter.add(
        new IgnoreExceptionsInterFilter<>(
            new IgnoreExceptionsFilter(TypeReference.JavaLangNegativeArraySizeException)));
  }

  @Test
  public void testIntra(final SoftAssertions softly) {
    for (CGNode node : cg) {
      IntraproceduralExceptionAnalysis analysis =
          new IntraproceduralExceptionAnalysis(node, filter.getFilter(node), cha, pointerAnalysis);

      if (node.getMethod()
          .getDeclaringClass()
          .getName()
          .getClassName()
          .toString()
          .equals("TestPruning")) {
        checkThrownExceptions(softly, node, analysis);
        checkCaughtExceptions(softly, node, analysis);
      }
    }
  }

  private void checkCaughtExceptions(
      final SoftAssertions softly, CGNode node, IntraproceduralExceptionAnalysis analysis) {
    Supplier<String> text =
        () ->
            String.format(
                "Number of caught exceptions did not match in %s. The follwoing exceptions were caught: ",
                node.getMethod().getName());
    Iterator<CallSiteReference> it = node.iterateCallSites();
    while (it.hasNext()) {
      Set<TypeReference> caught = analysis.getCaughtExceptions(it.next());
      final var caughtTypesAssertion =
          softly.assertThat(caught).withFailMessage(() -> text.get() + caught);
      if (node.getMethod().getName().toString().matches("testTryCatch.*")) {
        if (node.getMethod().getName().toString().equals("testTryCatchMultipleExceptions")) {
          caughtTypesAssertion.hasSize(2);
        } else if (node.getMethod().getName().toString().equals("testTryCatchSuper")) {
          caughtTypesAssertion.hasSizeGreaterThan(3);
        } else {
          caughtTypesAssertion.hasSize(1);
        }
      } else {
        caughtTypesAssertion.isEmpty();
      }
    }
  }

  private void checkThrownExceptions(
      final SoftAssertions softly, CGNode node, IntraproceduralExceptionAnalysis analysis) {
    Set<TypeReference> exceptions = analysis.getExceptions();
    Supplier<String> failMessage =
        () ->
            String.format(
                "Number of thrown exceptions did not match in %s. The follwoing exceptions were thrown: %s",
                node.getMethod().getName(), exceptions);

    if (node.getMethod().getName().toString().matches("invokeSingle.*")
        && !node.getMethod().getName().toString().equals("invokeSingleRecursive2Helper")
        && !node.getMethod().getName().toString().equals("invokeSinglePassThrough")) {
      softly.assertThat(exceptions).withFailMessage(failMessage).hasSize(1);
    } else {
      softly.assertThat(exceptions).withFailMessage(failMessage).isEmpty();
    }
  }

  @Test
  public void testInterprocedural(final SoftAssertions softly) {
    ExceptionAnalysis analysis = new ExceptionAnalysis(cg, pointerAnalysis, cha, filter);
    analysis.solve();

    for (CGNode node : cg) {
      if (node.getMethod()
          .getDeclaringClass()
          .getName()
          .getClassName()
          .toString()
          .equals("TestPruning")) {
        Set<TypeReference> exceptions = analysis.getCGNodeExceptions(node);
        Supplier<String> text =
            () ->
                String.format(
                    "Number of thrown exceptions did not match in %s. The follwoing exceptions were thrown: %s",
                    node.getMethod().getName(), exceptions);
        final var exceptionsAssertion = softly.assertThat(exceptions).withFailMessage(text);
        if (node.getMethod().getName().toString().matches("invokeSingle.*")) {
          exceptionsAssertion.hasSize(1);
        } else if (node.getMethod().getName().toString().matches("testTryCatch.*")) {
          exceptionsAssertion.isEmpty();
        } else if (node.getMethod().getName().toString().matches("invokeAll.*")) {
          exceptionsAssertion.hasSize(2);
        } else if (node.getMethod().getName().toString().equals("main")) {
          exceptionsAssertion.isEmpty();
        } else {
          softly
              .assertThat(node.getMethod().getName())
              .asString()
              .withFailMessage(
                  "Found method, i didn't know the expected number of exceptions for: %s",
                  node.getMethod().getName())
              .isIn("main", "<init>");
        }

        analysis.getCGNodeExceptions(node);
      }
    }
  }
}
