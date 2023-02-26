/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.ibm.wala.cast.java.test;

import static com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope.SOURCE;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ECJJava17IRTest extends IRTests {

  private static final String packageName = "javaonepointseven";

  public ECJJava17IRTest() {
    super(null);
    dump = true;
  }

  @Override
  protected AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, ?> getAnalysisEngine(
      final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
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

  private final IRAssertion checkBinaryLiterals =
      new IRAssertion() {
        private final TypeReference testClass =
            TypeReference.findOrCreate(
                SOURCE, TypeName.findOrCreateClassName(packageName, "BinaryLiterals"));

        private final Pair<MethodReference, int[]>[] constants =
            new Pair[] {
              Pair.make(
                  MethodReference.findOrCreate(testClass, MethodReference.clinitSelector),
                  new int[] {
                    0b00110001,
                    0b01100010,
                    0b11000100,
                    0b10001001,
                    0b00010011,
                    0b00100110,
                    0b01001100,
                    0b10011000
                  })
            };

        @Override
        public void check(CallGraph cg) {
          for (Pair<MethodReference, int[]> m : constants) {
            cg.getNodes(m.fst)
                .forEach(
                    (n) -> {
                      SymbolTable st = n.getIR().getSymbolTable();
                      for (int value : m.snd) {
                        check:
                        {
                          for (int i = 1; i <= st.getMaxValueNumber(); i++) {
                            if (st.isIntegerConstant(i) && st.getIntValue(i) == value) {
                              System.err.println("found " + value + " in " + n);
                              break check;
                            }
                          }
                          assert false : "cannot find " + value + " in " + n;
                        }
                      }
                    });
          }
        }
      };

  @Test
  public void testBinaryLiterals() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointseven"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointseven"),
        Collections.singletonList(checkBinaryLiterals),
        true,
        null);
  }

  private final IRAssertion checkCatchMultipleExceptionTypes =
      new IRAssertion() {

        private final TypeReference testClass =
            TypeReference.findOrCreate(
                SOURCE, TypeName.findOrCreateClassName(packageName, "CatchMultipleExceptionTypes"));

        private final MethodReference testMethod =
            MethodReference.findOrCreate(testClass, "test", "(I[I)V");

        @Override
        public void check(CallGraph cg) {
          Set<IClass> expectedTypes = HashSetFactory.make();
          expectedTypes.add(
              cg.getClassHierarchy().lookupClass(TypeReference.JavaLangArithmeticException));
          expectedTypes.add(
              cg.getClassHierarchy()
                  .lookupClass(
                      TypeReference.findOrCreate(
                          ClassLoaderReference.Primordial,
                          "Ljava/lang/IndexOutOfBoundsException")));

          cg.getNodes(testMethod)
              .forEach(
                  (n) ->
                      n.getIR()
                          .getControlFlowGraph()
                          .forEach(
                              (bb) -> {
                                if (bb.isCatchBlock()) {
                                  Set<IClass> foundTypes = HashSetFactory.make();
                                  bb.getCaughtExceptionTypes()
                                      .forEachRemaining(
                                          (t) ->
                                              foundTypes.add(
                                                  cg.getClassHierarchy().lookupClass(t)));

                                  assert foundTypes.equals(expectedTypes) : n.getIR();
                                }
                              }));
        }
      };

  @Test
  public void testCatchMultipleExceptionTypes()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointseven"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointseven"),
        Collections.singletonList(checkCatchMultipleExceptionTypes),
        true,
        null);
  }

  private static final List<IRAssertion> SiSAssertions =
      Collections.singletonList(
          new InstructionOperandAssertion(
              "Source#" + packageName + "/StringsInSwitch#main#([Ljava/lang/String;)V",
              t ->
                  (t instanceof SSAAbstractInvokeInstruction)
                      && t.toString().contains("getTypeOfDayWithSwitchStatement"),
              1,
              new int[] {9, 58, 9, 67}));

  @Test
  public void testStringsInSwitch() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointseven"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointseven"),
        SiSAssertions,
        true,
        null);
  }

  @Test
  public void testTryWithResourcesStatement()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointseven"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointseven"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testTypeInferenceforGenericInstanceCreation()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointseven"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointseven"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testUnderscoresInNumericLiterals()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc("javaonepointseven"),
        rtJar,
        simplePkgTestEntryPoint("javaonepointseven"),
        emptyList,
        true,
        null);
  }
}
