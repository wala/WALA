/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
/*
 * Created on Oct 21, 2005
 */
package com.ibm.wala.cast.java.test;

import static com.ibm.wala.ipa.slicer.SlicerUtil.dumpSlice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.java.ipa.slicer.AstJavaSlicer;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.util.CallGraphSearchUtil;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.SlicerUtil;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class JavaIRTests extends IRTests {

  static List<? extends IRAssertion> callAssertionForInterfaceTest1 =
      Arrays.asList(
          cg -> {
            final String typeStr = "IFoo";

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

            final IClass iClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(iClass).matches(IClass::isInterface);
          },
          cg -> {
            final String typeStr = "FooIT1";

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

            final IClass iClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(iClass).isNotNull();

            final Collection<? extends IClass> interfaces = iClass.getDirectInterfaces();

            assertThat(interfaces).hasSize(1);

            Assertions.<IClass>assertThatCollection(interfaces)
                .contains(
                    cg.getClassHierarchy()
                        .lookupClass(
                            findOrCreateTypeReference("Source", "IFoo", cg.getClassHierarchy())));
          });

  static List<? extends IRAssertion> callAssertionForInheritance1 =
      Collections.singletonList(
          cg -> {
            final String typeStr = "Derived";

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

            final IClass derivedClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(derivedClass).isNotNull();

            final TypeReference baseType =
                findOrCreateTypeReference("Source", "Base", cg.getClassHierarchy());
            final IClass baseClass = cg.getClassHierarchy().lookupClass(baseType);

            assertThat(derivedClass.getSuperclass())
                .as("Expected 'Base' to be the superclass of 'Derived'")
                .isEqualTo(baseClass);

            Collection<IClass> subclasses = cg.getClassHierarchy().computeSubClasses(baseType);

            assertThat(subclasses)
                .as("Expected subclasses of 'Base' to be 'Base' and 'Derived'.")
                .contains(derivedClass, baseClass);
          });

  static List<? extends IRAssertion> callAssertionForArrayLiteral1 =
      Collections.singletonList(
          cg -> {
            MethodReference mref =
                descriptorToMethodRef(
                    "Source#ArrayLiteral1#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

            CGNode node = cg.getNodes(mref).iterator().next();
            SSAInstruction s = node.getIR().getInstructions()[2];
            assertThat(s)
                .as("Did not find new array instruction.")
                .asInstanceOf(type(SSANewInstruction.class))
                .extracting(sni -> sni.getNewSite().getDeclaredType())
                .matches(TypeReference::isArrayType);
          });

  static List<? extends IRAssertion> callAssertionForQualifiedStatic =
      Collections.singletonList(
          cg -> {
            MethodReference mref =
                descriptorToMethodRef(
                    "Source#QualifiedStatic#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

            CGNode node = cg.getNodes(mref).iterator().next();
            SSAInstruction s = node.getIR().getInstructions()[4];

            final FieldReference field =
                assertThat(s)
                    .asInstanceOf(type(SSAGetInstruction.class))
                    .matches(SSAFieldAccessInstruction::isStatic)
                    .actual()
                    .getDeclaredField();
            assertThat(field.getName()).hasToString("value");
            assertThat(field.getDeclaringClass().getName()).hasToString("LFooQ");
          });

  static List<? extends IRAssertion> callAssertionForArrayLiteral2 =
      Collections.singletonList(
          cg -> {
            MethodReference mref =
                descriptorToMethodRef(
                    "Source#ArrayLiteral2#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

            CGNode node = cg.getNodes(mref).iterator().next();

            final SSAInstruction[] instructions = node.getIR().getInstructions();
            // test 1
            {
              SSAInstruction s1 = instructions[2];
              if (s1 instanceof SSANewInstruction) {
                assertThat(((SSANewInstruction) s1).getNewSite().getDeclaredType())
                    .matches(TypeReference::isArrayType);
              } else {
                fail("Expected 3rd to be a new array instruction.");
              }
            }
            // test 2
            {
              SSAInstruction s2 = instructions[3];
              if (s2 instanceof SSANewInstruction) {
                assertThat(((SSANewInstruction) s2).getNewSite().getDeclaredType())
                    .matches(TypeReference::isArrayType);
              } else {
                fail("Expected 4th to be a new array instruction.");
              }
            }
            // test 3: the last 4 instructions are of the form y[i] = i+1;
            {
              final SymbolTable symbolTable = node.getIR().getSymbolTable();
              for (int i = 4; i <= 7; i++) {

                SSAArrayStoreInstruction as =
                    assertThat(Arrays.stream(instructions))
                        .element(i)
                        .asInstanceOf(type(SSAArrayStoreInstruction.class))
                        .actual();

                assertThat(node.getIR().getLocalNames(i, as.getArrayRef())[0])
                    .as("Expected an array store to 'y'.")
                    .isEqualTo("y");

                final Integer valueOfArrayIndex =
                    ((Integer) symbolTable.getConstantValue(as.getIndex()));
                final Integer valueAssigned = (Integer) symbolTable.getConstantValue(as.getValue());

                assertThat(valueAssigned.intValue())
                    .as("Expected an array store to 'y' with value " + (valueOfArrayIndex + 1))
                    .isEqualTo(valueOfArrayIndex + 1);
              }
            }
          });

  static List<EdgeAssertions> edgeAssertions =
      Arrays.asList(
          EdgeAssertions.make(
              "Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#foo#()V"),
          EdgeAssertions.make(
              "Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#bar#()V"));
  static List<? extends IRAssertion> callAssertionForArray1 =
      Collections.singletonList(
          /*
           * 'foo' has four array instructions: - 2 SSAArrayLengthInstruction - 1
           * SSAArrayLoadInstruction - 1 SSAArrayStoreInstruction
           */
          new IRAssertion() {

            @Override
            public void check(CallGraph cg) {

              MethodReference mref =
                  descriptorToMethodRef("Source#Array1#foo#()V", cg.getClassHierarchy());

              int count = 0;
              CGNode node = cg.getNodes(mref).iterator().next();
              for (SSAInstruction s : node.getIR().getInstructions()) {
                if (isArrayInstruction(s)) {
                  count++;
                }
              }

              assertThat(count)
                  .as("Unexpected number of array instructions in 'foo'.")
                  .isEqualTo(4);
            }

            private boolean isArrayInstruction(SSAInstruction s) {
              return s instanceof SSAArrayReferenceInstruction
                  || s instanceof SSAArrayLengthInstruction;
            }
          });

  static List<? extends IRAssertion> callAssertionForSimple1 =
      Arrays.asList(
          new SourceMapAssertion("Source#Simple1#doStuff#(I)V", "prod", 24),
          new SourceMapAssertion("Source#Simple1#doStuff#(I)V", "j", 23),
          new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "s", 32),
          new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "i", 28),
          new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "sum", 29),
          EdgeAssertions.make(
              "Source#Simple1#main#([Ljava/lang/String;)V", "Source#Simple1#doStuff#(I)V"),
          EdgeAssertions.make(
              "Source#Simple1#instanceMethod1#()V", "Source#Simple1#instanceMethod2#()V"));

  static List<? extends IRAssertion> callAssertionForTwoClasses =
      List.of(
          cg -> {
            final String typeStr = singleInputForTest("TwoClasses");

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

            final IClass iClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(iClass).isNotNull();

            /*
            assertThat(iClass.getClassLoader().getNumberOfClasses()).isEqualTo(2);

            for (IClass cls : Iterator2Iterable.make(iClass.getClassLoader().iterateAllClasses())) {
              assertTrue("Expected class to be either " + typeStr + " or " + "Bar", cls.getName().getClassName().toString()
                  .equals(typeStr)
                  || cls.getName().getClassName().toString().equals("Bar"));
            }
            */
          });
  static List<? extends IRAssertion> callAssertionForStaticNesting =
      Collections.singletonList(
          cg -> {
            final String typeStr = singleInputForTest("StaticNesting") + "$WhatsIt";

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

            final IClass iClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(iClass).isNotNull();

            // todo: this fails: assertNotNull("Expected to be enclosed in
            // 'StaticNesting'.",
            // ((JavaSourceLoaderImpl.JavaClass)iClass).getEnclosingClass());
            // todo: is there the concept of CompilationUnit?

            /*
             * {@link JavaCAst2IRTranslator#getEnclosingType} return null for static inner
             * classes..?
             */
          });

  static List<? extends IRAssertion> callAssertionForInnerClass =
      Collections.singletonList(
          cg -> {
            final String typeStr = singleInputForTest("InnerClass");

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr + "$WhatsIt", cg.getClassHierarchy());

            final IClass iClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(iClass).isNotNull();

            assertThat(((JavaClass) iClass).getEnclosingClass())
                .as("Expected to be enclosed in 'InnerClass'.")
                .isEqualTo(
                    cg.getClassHierarchy()
                        .lookupClass(
                            findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy())));
          });

  static List<? extends IRAssertion> callAssertionForLocalClass =
      Collections.singletonList(
          cg -> {
            final String typeStr = singleInputForTest("LocalClass");
            final String localClassStr = "Foo";

            // Observe the descriptor for a class local to a method.
            final TypeReference mainFooType =
                findOrCreateTypeReference(
                    "Source",
                    typeStr + "/main([Ljava/lang/String;)V/" + localClassStr,
                    cg.getClassHierarchy());

            // Observe the descriptor for a class local to a method.
            final IClass mainFooClass = cg.getClassHierarchy().lookupClass(mainFooType);
            assertThat(mainFooClass).isNotNull();

            final TypeReference methodFooType =
                findOrCreateTypeReference(
                    "Source", typeStr + "/method()V/" + localClassStr, cg.getClassHierarchy());

            final IClass methodFooClass = cg.getClassHierarchy().lookupClass(methodFooType);
            assertThat(methodFooClass).isNotNull();

            final IClass localClass =
                cg.getClassHierarchy()
                    .lookupClass(
                        findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy()));

            assertThat(((JavaClass) methodFooClass).getEnclosingClass())
                .as("'Foo' is enclosed in 'Local'")
                .isSameAs(localClass);
            // todo: is this failing because 'main' is static?
            // assertSame("'Foo' is enclosed in 'Local'",
            // ((JavaSourceLoaderImpl.JavaClass)mainFooClass).getEnclosingClass(),
            // localClass);
          });
  static List<? extends IRAssertion> callAssertionForAnonymousClass =
      Collections.singletonList(
          cg -> {
            final String typeStr = singleInputForTest("AnonymousClass");

            final TypeReference type =
                findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

            final IClass iClass = cg.getClassHierarchy().lookupClass(type);
            assertThat(iClass).isNotNull();

            // todo what to check?? could not find anything in the APIs for
            // anonymous
          });

  private static final List<IRAssertion> MLAssertions =
      Collections.singletonList(
          new InstructionOperandAssertion(
              "Source#MiniaturList#main#([Ljava/lang/String;)V",
              t -> (t instanceof SSAAbstractInvokeInstruction) && t.toString().contains("cons"),
              1,
              new int[] {53, 38, 53, 60}));

  static Stream<Arguments> javaIRTestsParameters() {
    return Stream.of(
        Arguments.of("AnonymousClass", callAssertionForAnonymousClass, true, null),
        Arguments.of("Array1", callAssertionForArray1, true, null),
        Arguments.of("ArrayLiteral1", callAssertionForArrayLiteral1, true, null),
        Arguments.of("ArrayLiteral2", callAssertionForArrayLiteral2, true, null),
        Arguments.of("Breaks", emptyList, true, null),
        Arguments.of("CastFromNull", emptyList, true, null),
        Arguments.of("Casts", emptyList, true, null),
        Arguments.of("Exception1", emptyList, true, null),
        Arguments.of("Exception2", emptyList, true, null),
        Arguments.of("Finally1", emptyList, true, null),
        Arguments.of("Inheritance1", callAssertionForInheritance1, true, null),
        Arguments.of("InheritedField", edgeAssertions, true, null),
        Arguments.of("InnerClass", callAssertionForInnerClass, true, null),
        Arguments.of("InterfaceTest1", callAssertionForInterfaceTest1, true, null),
        Arguments.of("LexicalAccessOfMethodVariablesFromAnonymousClass", emptyList, true, null),
        Arguments.of("LocalClass", callAssertionForLocalClass, true, null),
        Arguments.of("MiniaturList", MLAssertions, true, null),
        Arguments.of("Monitor", emptyList, true, null),
        Arguments.of("NullArrayInit", emptyList, true, null),
        Arguments.of("QualifiedStatic", callAssertionForQualifiedStatic, true, null),
        Arguments.of("Scoping1", emptyList, true, null),
        Arguments.of("Scoping2", emptyList, true, null),
        Arguments.of("Simple1", callAssertionForSimple1, true, null),
        Arguments.of("StaticInitializers", emptyList, true, null),
        Arguments.of("StaticNesting", callAssertionForStaticNesting, true, null),
        Arguments.of("Switch1", emptyList, true, null),
        Arguments.of("Thread1", emptyList, true, null),
        Arguments.of("TwoClasses", callAssertionForTwoClasses, true, null),
        Arguments.of("WhileTest1", emptyList, true, null));
  }

  static Stream<Arguments> javaIRTestsParametersWithPackageName() {
    return Stream.of(
        Arguments.of("bugfixes", "DoWhileInCase", emptyList, true, null),
        Arguments.of("bugfixes", "VarDeclInSwitch", emptyList, true, null),
        Arguments.of("p", "NonPrimaryTopLevel", emptyList, true, null));
  }

  @ParameterizedTest(name = "java17IRTestName={0}, assertReachable={2}, exclusionsFile={3}")
  @MethodSource("javaIRTestsParameters")
  public void runJavaIRTests(
      String java17IRTestName, List<IRAssertion> ca, boolean assertReachable, String exclusionsFile)
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(java17IRTestName),
        rtJar,
        simpleTestEntryPoint(java17IRTestName),
        ca,
        assertReachable,
        exclusionsFile);
  }

  @ParameterizedTest
  @MethodSource("javaIRTestsParametersWithPackageName")
  public void runJavaIRTestsWithPackageName(
      String packageName,
      String testName,
      List<IRAssertion> callAssertion,
      boolean assertReachable,
      String exclusionsFile)
      throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singlePkgTestSrc(packageName, testName),
        rtJar,
        simplePkgTestEntryPoint(packageName, testName),
        callAssertion,
        assertReachable,
        exclusionsFile);
  }

  @Test
  public void testInnerClassA() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x = runTest("InnerClassA");

    // can't do an IRAssertion() -- we need the pointer analysis

    CallGraph cg = x.fst;
    PointerAnalysis<? extends InstanceKey> pa =
        ((PropagationCallGraphBuilder) x.snd).getPointerAnalysis();

    for (CGNode n : cg) {
      // assume in the test we have one enclosing instruction for each of the methods here.
      String methodSigs[] = {
        "InnerClassA$AB.getA_X_from_AB()I",
        "InnerClassA$AB.getA_X_thru_AB()I",
        "InnerClassA$AB$ABSubA.getA_X()I",
        "InnerClassA$AB$ABA$ABAA.getABA_X()I",
        "InnerClassA$AB$ABA$ABAA.getA_X()I",
        "InnerClassA$AB$ABA$ABAB.getABA_X()I",
        "InnerClassA$AB$ABSubA$ABSubAA.getABA_X()I",
        "InnerClassA$AB$ABSubA$ABSubAA.getA_X()I",
      };

      // each type suffixed by ","
      String ikConcreteTypeStrings[] = {
        "LInnerClassA,",
        "LInnerClassA,",
        "LInnerClassA,",
        "LInnerClassA$AB$ABSubA,LInnerClassA$AB$ABA,",
        "LInnerClassA,",
        "LInnerClassA$AB$ABA,",
        "LInnerClassA$AB$ABSubA,",
        "LInnerClassA,",
      };

      assertThat(methodSigs).hasSameSizeAs(ikConcreteTypeStrings);
      for (int i = 0; i < methodSigs.length; i++) {
        if (n.getMethod().getSignature().equals(methodSigs[i])) {
          // find enclosing instruction
          for (SSAInstruction instr : n.getIR().getInstructions()) {
            if (instr instanceof EnclosingObjectReference) {
              StringBuilder allIksBuilder = new StringBuilder();
              for (InstanceKey ik : pa.getPointsToSet(new LocalPointerKey(n, instr.getDef()))) {
                allIksBuilder.append(ik.getConcreteType().getName()).append(',');
              }
              // System.out.printf("in method %s, got ik %s\n", methodSigs[i], allIks);

              final String allIks = allIksBuilder.toString();
              assertThat(allIks)
                  .as(
                      "assertion failed: expecting ik "
                          + ikConcreteTypeStrings[i]
                          + " in method "
                          + methodSigs[i]
                          + ", got "
                          + allIks
                          + "\n")
                  .isEqualTo(ikConcreteTypeStrings[i]);

              break;
            }
          }
        }
      }
    }
  }

  @Test
  public void testInnerClassSuper() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x = runTest("InnerClassSuper");

    // can't do an IRAssertion() -- we need the pointer analysis

    CallGraph cg = x.fst;
    PointerAnalysis<? extends InstanceKey> pa =
        ((PropagationCallGraphBuilder) x.snd).getPointerAnalysis();

    for (CGNode n : cg) {
      if (n.getMethod().getSignature().equals("LInnerClassSuper$SuperOuter.test()V")) {
        // find enclosing instruction
        for (SSAInstruction instr : n.getIR().getInstructions()) {
          if (instr instanceof EnclosingObjectReference) {
            StringBuilder allIksBuilder = new StringBuilder();
            for (InstanceKey ik : pa.getPointsToSet(new LocalPointerKey(n, instr.getDef()))) {
              allIksBuilder.append(ik.getConcreteType().getName()).append(',');
            }
            final String allIks = allIksBuilder.toString();
            assertThat(allIks)
                .as("assertion failed: expecting ik \"LSub,\" in method, got \"" + allIks + "\"\n")
                .isEqualTo("LSub,");

            break;
          }
        }
      }
    }
  }

  private static MethodReference getSliceRootReference(
      String className, String methodName, String methodDescriptor) {
    TypeName clsName = TypeName.string2TypeName('L' + className.replace('.', '/'));
    TypeReference clsRef = TypeReference.findOrCreate(JavaSourceAnalysisScope.SOURCE, clsName);

    Atom nameAtom = Atom.findOrCreateUnicodeAtom(methodName);
    Descriptor descr = Descriptor.findOrCreateUTF8(Language.JAVA, methodDescriptor);

    return MethodReference.findOrCreate(clsRef, nameAtom, descr);
  }

  @Test
  public void testMiniaturSliceBug() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x = runTest("MiniaturSliceBug");

    PointerAnalysis<? extends InstanceKey> pa =
        ((PropagationCallGraphBuilder) x.snd).getPointerAnalysis();
    CallGraph cg = x.fst;

    // test partial slice
    MethodReference sliceRootRef =
        getSliceRootReference("MiniaturSliceBug", "validNonDispatchedCall", "(LIntWrapper;)V");
    Set<CGNode> roots = cg.getNodes(sliceRootRef);
    Pair<Collection<Statement>, SDG<? extends InstanceKey>> y =
        AstJavaSlicer.computeAssertionSlice(cg, pa, roots, false);
    Collection<Statement> slice = y.fst;
    dumpSlice(slice);
    assertThat(SlicerUtil.countAllocations(slice, false)).isEqualTo(0);
    assertThat(SlicerUtil.countPutfields(slice)).isEqualTo(1);

    // test slice from main
    sliceRootRef = getSliceRootReference("MiniaturSliceBug", "main", "([Ljava/lang/String;)V");
    roots = cg.getNodes(sliceRootRef);
    y = AstJavaSlicer.computeAssertionSlice(cg, pa, roots, false);
    slice = y.fst;
    // SlicerUtil.dumpSlice(slice);
    assertThat(SlicerUtil.countAllocations(slice, false)).isEqualTo(2);
    assertThat(SlicerUtil.countPutfields(slice)).isEqualTo(2);
  }

  @Test
  public void testThinSlice() throws CancelException, IOException {
    String testName = "MiniaturSliceBug";
    Collection<Path> sources = singleTestSrc(testName);
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x =
        runTest(sources, rtJar, new String[] {'L' + testName}, emptyList, true, null);

    PointerAnalysis<InstanceKey> pa = ((PropagationCallGraphBuilder) x.snd).getPointerAnalysis();
    CallGraph cg = x.fst;

    // we just run context-sensitive and context-insensitive thin slicing, to make sure
    // it doesn't crash
    Statement statement =
        SlicerUtil.findCallTo(CallGraphSearchUtil.findMainMethod(cg), "validNonDispatchedCall");
    AstJavaModRef<InstanceKey> modRef = new AstJavaModRef<>();
    SDG<InstanceKey> sdg =
        new SDG<>(
            cg,
            pa,
            modRef,
            Slicer.DataDependenceOptions.NO_BASE_PTRS,
            Slicer.ControlDependenceOptions.NONE);
    Collection<Statement> slice =
        AstJavaSlicer.computeBackwardSlice(sdg, Collections.singleton(statement));
    dumpSlice(slice);

    ThinSlicer ts = new ThinSlicer(cg, pa, modRef);
    slice = ts.computeBackwardThinSlice(statement);
    dumpSlice(slice);
  }

  @Test
  public void testExclusions(@TempDir final File tmpDir)
      throws IllegalArgumentException, CancelException, IOException {
    File exclusions =
        TemporaryFile.stringToFile(
            File.createTempFile("exl", "txt", tmpDir), "Exclusions.Excluded\n");
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x =
        runTest(
            singleTestSrc("Exclusions"),
            rtJar,
            simpleTestEntryPoint("Exclusions"),
            emptyList,
            true,
            exclusions.getAbsolutePath());
    IClassHierarchy cha = x.fst.getClassHierarchy();

    TypeReference topType =
        TypeReference.findOrCreate(
            JavaSourceAnalysisScope.SOURCE, TypeName.findOrCreate("LExclusions"));
    assert cha.lookupClass(topType) != null;

    TypeReference inclType =
        TypeReference.findOrCreate(
            JavaSourceAnalysisScope.SOURCE, TypeName.findOrCreate("LExclusions$Included"));
    assert cha.lookupClass(inclType) != null;

    TypeReference exclType =
        TypeReference.findOrCreate(
            JavaSourceAnalysisScope.SOURCE, TypeName.findOrCreate("LExclusions$Excluded"));
    assert cha.lookupClass(exclType) == null;
  }
}
