/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 21, 2005
 */
package com.ibm.wala.cast.java.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.ipa.slicer.AstJavaSlicer;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
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
import com.ibm.wala.util.strings.Atom;

public abstract class JavaIRTests extends IRTests {
   
  public JavaIRTests(String projectName)  {
    super(projectName);
  }
  
  public JavaIRTests() {
    this(null);
  }
 
  @Test public void testSimple1() throws IllegalArgumentException, CancelException, IOException {

    List<? extends IRAssertion> assertions = Arrays.asList(
        new SourceMapAssertion("Source#Simple1#doStuff#(I)V", "prod", 24),
        new SourceMapAssertion("Source#Simple1#doStuff#(I)V", "j", 23),
        new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "s", 32), 
        new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "i", 28), 
        new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "sum", 29), 
        EdgeAssertions.make("Source#Simple1#main#([Ljava/lang/String;)V", "Source#Simple1#doStuff#(I)V"), 
        EdgeAssertions.make("Source#Simple1#instanceMethod1#()V", "Source#Simple1#instanceMethod2#()V"));

    // this needs soure positions to work too
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), assertions, true);
  }

  @Test public void testTwoClasses() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

    cg -> {
      final String typeStr = singleInputForTest();

      final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

      final IClass iClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, iClass);

      /*
      Assert.assertEquals("Expected two classes.", iClass.getClassLoader().getNumberOfClasses(), 2);

      for (IClass cls : Iterator2Iterable.make(iClass.getClassLoader().iterateAllClasses())) {
        Assert.assertTrue("Expected class to be either " + typeStr + " or " + "Bar", cls.getName().getClassName().toString()
            .equals(typeStr)
            || cls.getName().getClassName().toString().equals("Bar"));
      }
      */
    }), true);
  }

  @Test public void testInterfaceTest1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

    cg -> {
      final String typeStr = "IFoo";

      final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

      final IClass iClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, iClass);

      Assert.assertTrue("Expected IFoo to be an interface.", iClass.isInterface());
    },

    cg -> {
      final String typeStr = "FooIT1";

      final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

      final IClass iClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, iClass);

      final Collection<? extends IClass> interfaces = iClass.getDirectInterfaces();

      Assert.assertEquals("Expected one single interface.", interfaces.size(), 1);

      Assert.assertTrue("Expected Foo to implement IFoo", interfaces.contains(cg.getClassHierarchy().lookupClass(
          findOrCreateTypeReference("Source", "IFoo", cg.getClassHierarchy()))));
    }), true);
  }

  @Test public void testInheritance1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
    cg -> {
      final String typeStr = "Derived";

      final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

      final IClass derivedClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, derivedClass);

      final TypeReference baseType = findOrCreateTypeReference("Source", "Base", cg.getClassHierarchy());
      final IClass baseClass = cg.getClassHierarchy().lookupClass(baseType);

      Assert.assertTrue("Expected 'Base' to be the superclass of 'Derived'", derivedClass.getSuperclass().equals(baseClass));

      Collection<IClass> subclasses = cg.getClassHierarchy().computeSubClasses(baseType);

      Assert.assertTrue("Expected subclasses of 'Base' to be 'Base' and 'Derived'.", subclasses.contains(derivedClass)
          && subclasses.contains(baseClass));
    }), true);
  }

  @Test public void testArray1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
    /**
     * 'foo' has four array instructions: - 2 SSAArrayLengthInstruction - 1
     * SSAArrayLoadInstruction - 1 SSAArrayStoreInstruction
     */
    new IRAssertion() {

      @Override
      public void check(CallGraph cg) {

        MethodReference mref = descriptorToMethodRef("Source#Array1#foo#()V", cg.getClassHierarchy());

        int count = 0;
        CGNode node = cg.getNodes(mref).iterator().next();
        for (SSAInstruction s : node.getIR().getInstructions()) {
          if (isArrayInstruction(s)) {
            count++;
          }
        }

        Assert.assertEquals("Unexpected number of array instructions in 'foo'.", count, 4);
      }

      private boolean isArrayInstruction(SSAInstruction s) {
        return s instanceof SSAArrayReferenceInstruction || s instanceof SSAArrayLengthInstruction;
      }
    }), true);
  }

  @Test public void testArrayLiteral1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
    cg -> {

      MethodReference mref = descriptorToMethodRef("Source#ArrayLiteral1#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

      CGNode node = cg.getNodes(mref).iterator().next();
      SSAInstruction s = node.getIR().getInstructions()[2];
      Assert.assertTrue("Did not find new array instruction.", s instanceof SSANewInstruction);
      Assert.assertTrue("", ((SSANewInstruction) s).getNewSite().getDeclaredType().isArrayType());
    }), true);
  }

  @Test public void testArrayLiteral2() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
    cg -> {

      MethodReference mref = descriptorToMethodRef("Source#ArrayLiteral2#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

      CGNode node = cg.getNodes(mref).iterator().next();

      final SSAInstruction[] instructions = node.getIR().getInstructions();
      // test 1
      {
        SSAInstruction s1 = instructions[2];
        if (s1 instanceof SSANewInstruction) {
          Assert.assertTrue("", ((SSANewInstruction) s1).getNewSite().getDeclaredType().isArrayType());
        } else {
          Assert.assertTrue("Expected 3rd to be a new array instruction.", false);
        }
      }
      // test 2
      {
        SSAInstruction s2 = instructions[3];
        if (s2 instanceof SSANewInstruction) {
          Assert.assertTrue("", ((SSANewInstruction) s2).getNewSite().getDeclaredType().isArrayType());
        } else {
          Assert.assertTrue("Expected 4th to be a new array instruction.", false);
        }
      }
      // test 3: the last 4 instructions are of the form y[i] = i+1;
      {
        final SymbolTable symbolTable = node.getIR().getSymbolTable();
        for (int i = 4; i <= 7; i++) {
          Assert.assertTrue("Expected only array stores.", instructions[i] instanceof SSAArrayStoreInstruction);

          SSAArrayStoreInstruction as = (SSAArrayStoreInstruction) instructions[i];

          Assert.assertEquals("Expected an array store to 'y'.", node.getIR().getLocalNames(i, as.getArrayRef())[0], "y");

          final Integer valueOfArrayIndex = ((Integer) symbolTable.getConstantValue(as.getIndex()));
          final Integer valueAssigned = (Integer) symbolTable.getConstantValue(as.getValue());

          Assert.assertEquals("Expected an array store to 'y' with value " + (valueOfArrayIndex + 1), valueAssigned.intValue(),
              valueOfArrayIndex + 1);

        }
      }
    }), true);
  }

  @Test public void testInheritedField() throws IllegalArgumentException, CancelException, IOException {
    List<EdgeAssertions> edgeAssertionses = Arrays.asList(EdgeAssertions.make("Source#InheritedField#main#([Ljava/lang/String;)V",
        "Source#B#foo#()V"), EdgeAssertions.make("Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#bar#()V"));
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), edgeAssertionses, true);
  }

  @Test public void testQualifiedStatic() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
    cg -> {

      MethodReference mref = descriptorToMethodRef("Source#QualifiedStatic#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

      CGNode node = cg.getNodes(mref).iterator().next();
      SSAInstruction s = node.getIR().getInstructions()[4];

      Assert.assertTrue("Did not find a getstatic instruction.", s instanceof SSAGetInstruction
          && ((SSAGetInstruction) s).isStatic());
      final FieldReference field = ((SSAGetInstruction) s).getDeclaredField();
      Assert.assertEquals("Expected a getstatic for 'value'.", field.getName().toString(), "value");
      Assert.assertEquals("Expected a getstatic for 'value'.", field.getDeclaringClass().getName().toString(), "LFooQ");
    }), true);
  }

  @Test public void testStaticNesting() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

    cg -> {
      final String typeStr = singleInputForTest() + "$WhatsIt";

      final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

      final IClass iClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, iClass);

      // todo: this fails: Assert.assertNotNull("Expected to be enclosed in
      // 'StaticNesting'.",
      // ((JavaSourceLoaderImpl.JavaClass)iClass).getEnclosingClass());
      // todo: is there the concept of CompilationUnit?

      /**
       * {@link JavaCAst2IRTranslator#getEnclosingType} return null for static
       * inner classes..?
       */
    }), true);
  }

  @Test public void testCastFromNull() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<IRAssertion>(), true);
  }
  
  @Test public void testInnerClass() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

    cg -> {
      final String typeStr = singleInputForTest();

      final TypeReference type = findOrCreateTypeReference("Source", typeStr + "$WhatsIt", cg.getClassHierarchy());

      final IClass iClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, iClass);

      Assert.assertEquals("Expected to be enclosed in 'InnerClass'.", ((JavaSourceLoaderImpl.JavaClass) iClass)
          .getEnclosingClass(), // todo is there another way?
          cg.getClassHierarchy().lookupClass(findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy())));
    }), true);
  }

  @Test public void testNullArrayInit() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<IRAssertion>(), true);
  }

  @Test public void testInnerClassA() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, PointerAnalysis<InstanceKey>> x =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<IRAssertion>(), true);
    
    // can't do an IRAssertion() -- we need the pointer analysis
    
    CallGraph cg = x.fst;
    PointerAnalysis<InstanceKey> pa = x.snd;

    Iterator<CGNode> iter = cg.iterator();
    while ( iter.hasNext() ) {
     CGNode n = iter.next();

          // assume in the test we have one enclosing instruction for each of the methods here.
          String methodSigs[] = { "InnerClassA$AB.getA_X_from_AB()I",
                  "InnerClassA$AB.getA_X_thru_AB()I",
                  "InnerClassA$AB$ABSubA.getA_X()I",
                  "InnerClassA$AB$ABA$ABAA.getABA_X()I",
                  "InnerClassA$AB$ABA$ABAA.getA_X()I",
                  "InnerClassA$AB$ABA$ABAB.getABA_X()I",
                  "InnerClassA$AB$ABSubA$ABSubAA.getABA_X()I",
                  "InnerClassA$AB$ABSubA$ABSubAA.getA_X()I", };

          // each type suffixed by ","
          String ikConcreteTypeStrings[ ]= {
                  "LInnerClassA,",
                  "LInnerClassA,",
                  "LInnerClassA,",
                  "LInnerClassA$AB$ABSubA,LInnerClassA$AB$ABA,",
                  "LInnerClassA,",
                  "LInnerClassA$AB$ABA,",
                  "LInnerClassA$AB$ABSubA,",
                  "LInnerClassA,",
          };

          Assert.assertTrue ( "Buggy test", methodSigs.length == ikConcreteTypeStrings.length );
          for ( int i = 0; i < methodSigs.length; i++ ) {
              if ( n.getMethod().getSignature().equals(methodSigs[i]) ) {
                  // find enclosing instruction
                  for ( SSAInstruction instr: n.getIR().getInstructions() ) {
                      if ( instr instanceof EnclosingObjectReference ) {
                          String allIks = "";
                          for (InstanceKey ik: pa.getPointsToSet(new LocalPointerKey(n,instr.getDef())))
                              allIks += ik.getConcreteType().getName() +",";
                          // System.out.printf("in method %s, got ik %s\n", methodSigs[i], allIks);
                          
                          Assert.assertTrue("assertion failed: expecting ik " + ikConcreteTypeStrings[i] + " in method " + methodSigs[i] +  ", got " + allIks + "\n",
                              allIks.equals(ikConcreteTypeStrings[i]));
                          
                          break;
                      }
                  }
              }
          }
      }


  }

  @Test public void testInnerClassSuper() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, PointerAnalysis<InstanceKey>> x =
        runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), new ArrayList<IRAssertion>(), true);
    
    // can't do an IRAssertion() -- we need the pointer analysis
    
    CallGraph cg = x.fst;
    PointerAnalysis<InstanceKey> pa = x.snd;

    Iterator<CGNode> iter = cg.iterator();
    while ( iter.hasNext() ) {
      CGNode n = iter.next();
      if ( n.getMethod().getSignature().equals("LInnerClassSuper$SuperOuter.test()V") ) {
        // find enclosing instruction
        for ( SSAInstruction instr: n.getIR().getInstructions() ) {
          if ( instr instanceof EnclosingObjectReference ) {
            String allIks = "";
            for (InstanceKey ik: pa.getPointsToSet(new LocalPointerKey(n,instr.getDef())))
              allIks += ik.getConcreteType().getName() +",";
            Assert.assertTrue("assertion failed: expecting ik \"LSub,\" in method, got \"" + allIks + "\"\n",
                allIks.equals("LSub,"));

            break;
          }
        }
      }
    }


  }

  @Test public void testLocalClass() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

    cg -> {
      final String typeStr = singleInputForTest();
      final String localClassStr = "Foo";

      // Observe the descriptor for a class local to a method.
      final TypeReference mainFooType = findOrCreateTypeReference("Source", typeStr + "/main([Ljava/lang/String;)V/"
          + localClassStr, cg.getClassHierarchy());

      // Observe the descriptor for a class local to a method.
      final IClass mainFooClass = cg.getClassHierarchy().lookupClass(mainFooType);
      Assert.assertNotNull("Could not find class " + mainFooType, mainFooClass);

      final TypeReference methodFooType = findOrCreateTypeReference("Source", typeStr + "/method()V/" + localClassStr, cg
          .getClassHierarchy());

      final IClass methodFooClass = cg.getClassHierarchy().lookupClass(methodFooType);
      Assert.assertNotNull("Could not find class " + methodFooType, methodFooClass);

      final IClass localClass = cg.getClassHierarchy().lookupClass(
          findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy()));

      Assert.assertSame("'Foo' is enclosed in 'Local'", ((JavaSourceLoaderImpl.JavaClass) methodFooClass).getEnclosingClass(),
          localClass);
      // todo: is this failing because 'main' is static?
      // Assert.assertSame("'Foo' is enclosed in 'Local'",
      // ((JavaSourceLoaderImpl.JavaClass)mainFooClass).getEnclosingClass(),
      // localClass);
    }), true);
  }

  @Test public void testAnonymousClass() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

    cg -> {
      final String typeStr = singleInputForTest();

      final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

      final IClass iClass = cg.getClassHierarchy().lookupClass(type);
      Assert.assertNotNull("Could not find class " + typeStr, iClass);

      // todo what to check?? could not find anything in the APIs for
      // anonymous
    }), true);
  }

  @Test public void testWhileTest1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testSwitch1() {
    try {
      runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CancelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test public void testException1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testException2() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testFinally1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testScoping1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testScoping2() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testNonPrimaryTopLevel() throws IllegalArgumentException, CancelException, IOException {
    runTest(singlePkgTestSrc("p"), rtJar, simplePkgTestEntryPoint("p"), emptyList, true);
  }

  @Test public void testMiniaturList() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testMonitor() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testStaticInitializers() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testThread1() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testCasts() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  @Test public void testBreaks() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  private static MethodReference getSliceRootReference(String className, String methodName, String methodDescriptor) {
    TypeName clsName = TypeName.string2TypeName("L" + className.replace('.', '/'));
    TypeReference clsRef = TypeReference.findOrCreate(JavaSourceAnalysisScope.SOURCE, clsName);

    Atom nameAtom = Atom.findOrCreateUnicodeAtom(methodName);
    Descriptor descr = Descriptor.findOrCreateUTF8(Language.JAVA, methodDescriptor);

    return MethodReference.findOrCreate(clsRef, nameAtom, descr);
  }

  @Test public void testMiniaturSliceBug() throws IllegalArgumentException, CancelException, IOException {
    Pair<CallGraph, PointerAnalysis<InstanceKey>> x = runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);

    PointerAnalysis<InstanceKey> pa = x.snd;
    CallGraph cg = x.fst;

    // test partial slice
    MethodReference sliceRootRef = getSliceRootReference("MiniaturSliceBug", "validNonDispatchedCall", "(LIntWrapper;)V");
    Set<CGNode> roots = cg.getNodes(sliceRootRef);
    Pair<Collection<Statement>,SDG<InstanceKey>> y = AstJavaSlicer.computeAssertionSlice(cg, pa, roots, false);
    Collection<Statement> slice = y.fst;
    SlicerTest.dumpSlice(slice);
    Assert.assertEquals(0, SlicerTest.countAllocations(slice));
    Assert.assertEquals(1, SlicerTest.countPutfields(slice));

    // test slice from main
    sliceRootRef = getSliceRootReference("MiniaturSliceBug", "main", "([Ljava/lang/String;)V");
    roots = cg.getNodes(sliceRootRef);
    y = AstJavaSlicer.computeAssertionSlice(cg, pa, roots, false);
    slice = y.fst;
    // SlicerTest.dumpSlice(slice);
    Assert.assertEquals(2, SlicerTest.countAllocations(slice));
    Assert.assertEquals(2, SlicerTest.countPutfields(slice));
  }
  
  @Test public void testDoWhileInCase() throws IllegalArgumentException, CancelException, IOException {
    runTest(singleTestSrc("bugfixes"), rtJar, simplePkgTestEntryPoint("bugfixes"), emptyList, true); 
  }

}
