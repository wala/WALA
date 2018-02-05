package com.ibm.wala.core.tests.exceptionpruning;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.ibm.wala.analysis.exceptionanalysis.ExceptionAnalysis;
import com.ibm.wala.analysis.exceptionanalysis.IntraproceduralExceptionAnalysis;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
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
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * This class checks, if the number of exceptions which might occur intra and
 * interprocedural is right. As well as the number of caught exceptions for each
 * call site.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ExceptionAnalysisTest {
  private static ClassLoader CLASS_LOADER = ExceptionAnalysisTest.class.getClassLoader();
  public static String REGRESSION_EXCLUSIONS = "Java60RegressionExclusions.txt";

  private static ClassHierarchy cha;
  private static CallGraph cg;
  private static PointerAnalysis<InstanceKey> pointerAnalysis;
  private static CombinedInterproceduralExceptionFilter<SSAInstruction> filter;

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @BeforeClass
  public static void init() throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException {
    AnalysisOptions options;
    AnalysisScope scope;

    scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, new File(REGRESSION_EXCLUSIONS), CLASS_LOADER);
    cha = ClassHierarchyFactory.make(scope);

    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha, "Lexceptionpruning/TestPruning");
    options = new AnalysisOptions(scope, entrypoints);
    options.getSSAOptions().setPiNodePolicy(new AllIntegerDueToBranchePiPolicy());

    ReferenceCleanser.registerClassHierarchy(cha);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    ReferenceCleanser.registerCache(cache);
    CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
    cg = builder.makeCallGraph(options, null);
    pointerAnalysis = builder.getPointerAnalysis();

    /*
     * We will ignore some exceptions to focus on the exceptions we want to
     * raise (OwnException, ArrayIndexOutOfBoundException)
     */
    filter = new CombinedInterproceduralExceptionFilter<>();
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(TypeReference.JavaLangOutOfMemoryError)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangNullPointerException)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangExceptionInInitializerError)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangExceptionInInitializerError)));
    filter.add(new IgnoreExceptionsInterFilter<>(new IgnoreExceptionsFilter(
        TypeReference.JavaLangNegativeArraySizeException)));
  }

  @Test
  public void testIntra() {
    for (CGNode node : cg) {
      IntraproceduralExceptionAnalysis analysis = new IntraproceduralExceptionAnalysis(node, filter.getFilter(node), cha,
          pointerAnalysis);

      if (node.getMethod().getDeclaringClass().getName().getClassName().toString().equals("TestPruning")) {
        checkThrownExceptions(node, analysis);
        checkCaughtExceptions(node, analysis);
      }
    }
  }

  private void checkCaughtExceptions(CGNode node, IntraproceduralExceptionAnalysis analysis) {
    String text = "Number of caught exceptions did not match in " + node.getMethod().getName().toString()
        + ". The follwoing exceptions were caught: ";
    Iterator<CallSiteReference> it = node.iterateCallSites();
    while (it.hasNext()) {
      Set<TypeReference> caught = analysis.getCaughtExceptions(it.next());
      if (node.getMethod().getName().toString().matches("testTryCatch.*")) {
        if (node.getMethod().getName().toString().equals("testTryCatchMultipleExceptions")) {
          collector.checkThat(text + caught.toString(), caught.size(), equalTo(2));
        } else if (node.getMethod().getName().toString().equals("testTryCatchSuper")) {
          collector.checkThat(text + caught.toString(), caught.size(), not(anyOf(equalTo(0), equalTo(1), equalTo(2), equalTo(3))));
        } else {
          collector.checkThat(text + caught.toString(), caught.size(), equalTo(1));
        }
      } else {
        collector.checkThat(text + caught.toString(), caught.size(), equalTo(0));
      }
    }
  }

  private void checkThrownExceptions(CGNode node, IntraproceduralExceptionAnalysis analysis) {
    Set<TypeReference> exceptions = analysis.getExceptions();
    String text = "Number of thrown exceptions did not match in " + node.getMethod().getName().toString()
        + ". The follwoing exceptions were thrown: " + exceptions.toString();

    if (node.getMethod().getName().toString().matches("invokeSingle.*")
        && (!node.getMethod().getName().toString().equals("invokeSingleRecursive2Helper"))
        && (!node.getMethod().getName().toString().equals("invokeSinglePassThrough"))) {
      collector.checkThat(text, exceptions.size(), equalTo(1));
    } else {
      collector.checkThat(text, exceptions.size(), equalTo(0));
    }
  }

  @Test
  public void testInterprocedural() {
    ExceptionAnalysis analysis = new ExceptionAnalysis(cg, pointerAnalysis, cha, filter);
    analysis.solve();

    for (CGNode node : cg) {
      if (node.getMethod().getDeclaringClass().getName().getClassName().toString().equals("TestPruning")) {
        Set<TypeReference> exceptions = analysis.getCGNodeExceptions(node);
        String text = "Number of thrown exceptions did not match in " + node.getMethod().getName().toString()
            + ". The follwoing exceptions were thrown: " + exceptions.toString();
        if (node.getMethod().getName().toString().matches("invokeSingle.*")) {
          collector.checkThat(text, exceptions.size(), equalTo(1));
        } else if (node.getMethod().getName().toString().matches("testTryCatch.*")) {
          collector.checkThat(text, exceptions.size(), equalTo(0));
        } else if (node.getMethod().getName().toString().matches("invokeAll.*")) {
          collector.checkThat(text, exceptions.size(), equalTo(2));
        } else if (node.getMethod().getName().toString().equals("main")) {
          collector.checkThat(text, exceptions.size(), equalTo(0));
        } else {
          String text2 = "Found method, i didn't know the expected number of exceptions for: "
              + node.getMethod().getName().toString();
          collector.checkThat(text2, node.getMethod().getName().toString(), anyOf(equalTo("main"), equalTo("<init>")));
        }

        analysis.getCGNodeExceptions(node);
      }
    }
  }
}
