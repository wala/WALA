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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public abstract class JavaIRTests extends IRTests {

  public JavaIRTests(String projectName) {
    super(projectName);
  }

  public JavaIRTests() {
    this(null);
  }

  @Test
  public void testSimple1() throws IllegalArgumentException, CancelException, IOException {

    List<? extends IRAssertion> assertions =
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

    // this needs soure positions to work too
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), assertions, true, null);
  }

  @Test
  public void testTwoClasses() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        List.of(
            cg -> {
              final String typeStr = singleInputForTest();

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

              final IClass iClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(iClass, "Could not find class " + typeStr);

              /*
              assertEquals("Expected two classes.", iClass.getClassLoader().getNumberOfClasses(), 2);

              for (IClass cls : Iterator2Iterable.make(iClass.getClassLoader().iterateAllClasses())) {
                assertTrue("Expected class to be either " + typeStr + " or " + "Bar", cls.getName().getClassName().toString()
                    .equals(typeStr)
                    || cls.getName().getClassName().toString().equals("Bar"));
              }
              */
            }),
        true,
        null);
  }

  @Test
  public void testInterfaceTest1() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Arrays.asList(
            cg -> {
              final String typeStr = "IFoo";

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

              final IClass iClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(iClass, "Could not find class " + typeStr);

              assertTrue(iClass.isInterface(), "Expected IFoo to be an interface.");
            },
            cg -> {
              final String typeStr = "FooIT1";

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

              final IClass iClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(iClass, "Could not find class " + typeStr);

              final Collection<? extends IClass> interfaces = iClass.getDirectInterfaces();

              assertEquals(interfaces.size(), 1, "Expected one single interface.");

              assertTrue(
                  interfaces.contains(
                      cg.getClassHierarchy()
                          .lookupClass(
                              findOrCreateTypeReference("Source", "IFoo", cg.getClassHierarchy()))),
                  "Expected Foo to implement IFoo");
            }),
        true,
        null);
  }

  @Test
  public void testInheritance1() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              final String typeStr = "Derived";

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

              final IClass derivedClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(derivedClass, "Could not find class " + typeStr);

              final TypeReference baseType =
                  findOrCreateTypeReference("Source", "Base", cg.getClassHierarchy());
              final IClass baseClass = cg.getClassHierarchy().lookupClass(baseType);

              assertEquals(
                  derivedClass.getSuperclass(),
                  baseClass,
                  "Expected 'Base' to be the superclass of 'Derived'");

              Collection<IClass> subclasses = cg.getClassHierarchy().computeSubClasses(baseType);

              assertTrue(
                  subclasses.contains(derivedClass) && subclasses.contains(baseClass),
                  "Expected subclasses of 'Base' to be 'Base' and 'Derived'.");
            }),
        true,
        null);
  }

  @Test
  public void testArray1() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
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

                assertEquals(count, 4, "Unexpected number of array instructions in 'foo'.");
              }

              private boolean isArrayInstruction(SSAInstruction s) {
                return s instanceof SSAArrayReferenceInstruction
                    || s instanceof SSAArrayLengthInstruction;
              }
            }),
        true,
        null);
  }

  @Test
  public void testArrayLiteral1() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              MethodReference mref =
                  descriptorToMethodRef(
                      "Source#ArrayLiteral1#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

              CGNode node = cg.getNodes(mref).iterator().next();
              SSAInstruction s = node.getIR().getInstructions()[2];
              assertInstanceOf(SSANewInstruction.class, s, "Did not find new array instruction.");
              assertTrue(((SSANewInstruction) s).getNewSite().getDeclaredType().isArrayType(), "");
            }),
        true,
        null);
  }

  @Test
  public void testArrayLiteral2() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
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
                  assertTrue(
                      ((SSANewInstruction) s1).getNewSite().getDeclaredType().isArrayType(), "");
                } else {
                  fail("Expected 3rd to be a new array instruction.");
                }
              }
              // test 2
              {
                SSAInstruction s2 = instructions[3];
                if (s2 instanceof SSANewInstruction) {
                  assertTrue(
                      ((SSANewInstruction) s2).getNewSite().getDeclaredType().isArrayType(), "");
                } else {
                  fail("Expected 4th to be a new array instruction.");
                }
              }
              // test 3: the last 4 instructions are of the form y[i] = i+1;
              {
                final SymbolTable symbolTable = node.getIR().getSymbolTable();
                for (int i = 4; i <= 7; i++) {
                  assertInstanceOf(
                      SSAArrayStoreInstruction.class,
                      instructions[i],
                      "Expected only array stores.");

                  SSAArrayStoreInstruction as = (SSAArrayStoreInstruction) instructions[i];

                  assertEquals(
                      node.getIR().getLocalNames(i, as.getArrayRef())[0],
                      "y",
                      "Expected an array store to 'y'.");

                  final Integer valueOfArrayIndex =
                      ((Integer) symbolTable.getConstantValue(as.getIndex()));
                  final Integer valueAssigned =
                      (Integer) symbolTable.getConstantValue(as.getValue());

                  assertEquals(
                      valueAssigned.intValue(),
                      valueOfArrayIndex + 1,
                      "Expected an array store to 'y' with value " + (valueOfArrayIndex + 1));
                }
              }
            }),
        true,
        null);
  }

  @Test
  public void testInheritedField() throws IllegalArgumentException, CancelException, IOException {
    List<EdgeAssertions> edgeAssertionses =
        Arrays.asList(
            EdgeAssertions.make(
                "Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#foo#()V"),
            EdgeAssertions.make(
                "Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#bar#()V"));
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), edgeAssertionses, true, null);
  }

  @Test
  public void testQualifiedStatic() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              MethodReference mref =
                  descriptorToMethodRef(
                      "Source#QualifiedStatic#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

              CGNode node = cg.getNodes(mref).iterator().next();
              SSAInstruction s = node.getIR().getInstructions()[4];

              assertTrue(
                  s instanceof SSAGetInstruction && ((SSAGetInstruction) s).isStatic(),
                  "Did not find a getstatic instruction.");
              final FieldReference field = ((SSAGetInstruction) s).getDeclaredField();
              assertEquals(
                  field.getName().toString(), "value", "Expected a getstatic for 'value'.");
              assertEquals(
                  field.getDeclaringClass().getName().toString(),
                  "LFooQ",
                  "Expected a getstatic for 'value'.");
            }),
        true,
        null);
  }

  @Test
  public void testStaticNesting() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              final String typeStr = singleInputForTest() + "$WhatsIt";

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

              final IClass iClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(iClass, "Could not find class " + typeStr);

              // todo: this fails: assertNotNull("Expected to be enclosed in
              // 'StaticNesting'.",
              // ((JavaSourceLoaderImpl.JavaClass)iClass).getEnclosingClass());
              // todo: is there the concept of CompilationUnit?

              /*
               * {@link JavaCAst2IRTranslator#getEnclosingType} return null for static inner
               * classes..?
               */
            }),
        true,
        null);
  }

  @Test
  public void testCastFromNull() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<>(), true, null);
  }

  @Test
  public void testInnerClass() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              final String typeStr = singleInputForTest();

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr + "$WhatsIt", cg.getClassHierarchy());

              final IClass iClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(iClass, "Could not find class " + typeStr);

              assertEquals(
                  ((JavaClass) iClass).getEnclosingClass(),
                  cg.getClassHierarchy()
                      .lookupClass(
                          findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy())),
                  "Expected to be enclosed in 'InnerClass'.");
            }),
        true,
        null);
  }

  @Test
  public void testNullArrayInit() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<>(), true, null);
  }

  @Test
  public void testInnerClassA() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<>(), true, null);

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

      assertEquals(methodSigs.length, ikConcreteTypeStrings.length, "Buggy test");
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
              assertEquals(
                  allIks,
                  ikConcreteTypeStrings[i],
                  "assertion failed: expecting ik "
                      + ikConcreteTypeStrings[i]
                      + " in method "
                      + methodSigs[i]
                      + ", got "
                      + allIks
                      + "\n");

              break;
            }
          }
        }
      }
    }
  }

  @Test
  public void testInnerClassSuper() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<>(), true, null);

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
            assertEquals(
                "LSub,",
                allIks,
                "assertion failed: expecting ik \"LSub,\" in method, got \"" + allIks + "\"\n");

            break;
          }
        }
      }
    }
  }

  @Test
  public void testLocalClass() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              final String typeStr = singleInputForTest();
              final String localClassStr = "Foo";

              // Observe the descriptor for a class local to a method.
              final TypeReference mainFooType =
                  findOrCreateTypeReference(
                      "Source",
                      typeStr + "/main([Ljava/lang/String;)V/" + localClassStr,
                      cg.getClassHierarchy());

              // Observe the descriptor for a class local to a method.
              final IClass mainFooClass = cg.getClassHierarchy().lookupClass(mainFooType);
              assertNotNull(mainFooClass, "Could not find class " + mainFooType);

              final TypeReference methodFooType =
                  findOrCreateTypeReference(
                      "Source", typeStr + "/method()V/" + localClassStr, cg.getClassHierarchy());

              final IClass methodFooClass = cg.getClassHierarchy().lookupClass(methodFooType);
              assertNotNull(methodFooClass, "Could not find class " + methodFooType);

              final IClass localClass =
                  cg.getClassHierarchy()
                      .lookupClass(
                          findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy()));

              assertSame(
                  ((JavaClass) methodFooClass).getEnclosingClass(),
                  localClass,
                  "'Foo' is enclosed in 'Local'");
              // todo: is this failing because 'main' is static?
              // assertSame("'Foo' is enclosed in 'Local'",
              // ((JavaSourceLoaderImpl.JavaClass)mainFooClass).getEnclosingClass(),
              // localClass);
            }),
        true,
        null);
  }

  @Test
  public void testAnonymousClass() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc(),
        rtJar,
        simpleTestEntryPoint(),
        Collections.singletonList(
            cg -> {
              final String typeStr = singleInputForTest();

              final TypeReference type =
                  findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

              final IClass iClass = cg.getClassHierarchy().lookupClass(type);
              assertNotNull(iClass, "Could not find class " + typeStr);

              // todo what to check?? could not find anything in the APIs for
              // anonymous
            }),
        true,
        null);
  }

  @Test
  public void testWhileTest1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testSwitch1() {
    try {
      runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
    } catch (IllegalArgumentException | IOException | CancelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void testException1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testException2() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testFinally1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testScoping1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testScoping2() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testNonPrimaryTopLevel()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(singlePkgTestSrc("p"), rtJar, simplePkgTestEntryPoint("p"), emptyList, true, null);
  }

  private static final List<IRAssertion> MLAssertions =
      Collections.singletonList(
          new InstructionOperandAssertion(
              "Source#MiniaturList#main#([Ljava/lang/String;)V",
              t -> (t instanceof SSAAbstractInvokeInstruction) && t.toString().contains("cons"),
              1,
              new int[] {53, 38, 53, 60}));

  @Test
  public void testMiniaturList() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), MLAssertions, true, null);
  }

  @Test
  public void testMonitor() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testStaticInitializers()
      throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testThread1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testCasts() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }

  @Test
  public void testBreaks() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
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
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);

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
    assertEquals(0, SlicerUtil.countAllocations(slice, false));
    assertEquals(1, SlicerUtil.countPutfields(slice));

    // test slice from main
    sliceRootRef = getSliceRootReference("MiniaturSliceBug", "main", "([Ljava/lang/String;)V");
    roots = cg.getNodes(sliceRootRef);
    y = AstJavaSlicer.computeAssertionSlice(cg, pa, roots, false);
    slice = y.fst;
    // SlicerUtil.dumpSlice(slice);
    assertEquals(2, SlicerUtil.countAllocations(slice, false));
    assertEquals(2, SlicerUtil.countPutfields(slice));
  }

  @Test
  public void testThinSlice() throws CancelException, IOException {
    String testName = "MiniaturSliceBug";
    List<String> sources =
        Collections.singletonList(getTestSrcPath() + File.separator + testName + ".java");
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
  public void testDoWhileInCase() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc("bugfixes"),
        rtJar,
        simplePkgTestEntryPoint("bugfixes"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testVarDeclInSwitch() throws IllegalArgumentException, CancelException, IOException {
    runTest(
        singleTestSrc("bugfixes"),
        rtJar,
        simplePkgTestEntryPoint("bugfixes"),
        emptyList,
        true,
        null);
  }

  @Test
  public void testExclusions(@TempDir final File tmpDir)
      throws IllegalArgumentException, CancelException, IOException {
    File exclusions =
        TemporaryFile.stringToFile(
            File.createTempFile("exl", "txt", tmpDir), "Exclusions.Excluded\n");
    Pair<CallGraph, CallGraphBuilder<? super InstanceKey>> x =
        runTest(
            singleTestSrc(),
            rtJar,
            simpleTestEntryPoint(),
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

  @Test
  public void testLexicalAccessOfMethodVariablesFromAnonymousClass()
      throws CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true, null);
  }
}
