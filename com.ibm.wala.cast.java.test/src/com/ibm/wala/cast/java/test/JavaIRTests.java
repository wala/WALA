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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;


import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.slicer.AstJavaSlicer;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.eclipse.util.CancelException;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
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
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.Pair;



public class JavaIRTests extends IRTests {
  public JavaIRTests(String name) {
    super(name);
  }

  protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
    JavaSourceAnalysisEngine engine = new JavaSourceAnalysisEngine() {
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
        return Util.makeMainEntrypoints(EclipseProjectPath.SOURCE_REF, cha, mainClassDescriptors);
      }
    };
    engine.setExclusionsFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    return engine;
  }

  protected String singleInputForTest() {
    return getName().substring(4) + ".java";
  }

  protected String singleInputForTestNoExt() {
    return getName().substring(4);
  }

  protected String singlePkgInputForTest(String pkgName) {
    return pkgName + File.separator + getName().substring(4) + ".java";
  }

  public void testSimple1() {

    List<? extends IRAssertion> assertions = Arrays.asList(
      new SourceMapAssertion("Source#Simple1#doStuff#(I)V", "prod", 14),
      new SourceMapAssertion("Source#Simple1#doStuff#(I)V", "j", 13),
      new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "s", 22),
      new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "i", 18),
      new SourceMapAssertion("Source#Simple1#main#([Ljava/lang/String;)V", "sum", 19),
      EdgeAssertions.make("Source#Simple1#main#([Ljava/lang/String;)V", "Source#Simple1#doStuff#(I)V"),
      EdgeAssertions.make("Source#Simple1#instanceMethod1#()V", "Source#Simple1#instanceMethod2#()V"));

    // this needs soure positions to work too
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), assertions, true);
  }

  public void testTwoClasses() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

      new IRAssertion() {

        public void check(CallGraph cg)
        throws Exception {
          final String typeStr = singleInputForTestNoExt();

          final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

          final IClass iClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, iClass);

          Assert.assertEquals("Expected two classes.", iClass.getClassLoader().getNumberOfClasses(), 2);

          for(Iterator<IClass> it = iClass.getClassLoader().iterateAllClasses(); it.hasNext(); ) {
            IClass cls = it.next();

            Assert.assertTrue("Expected class to be either " + typeStr + " or " + "Bar",
              cls.getName().getClassName().toString().equals(typeStr) ||
              cls.getName().getClassName().toString().equals("Bar"));
          }
        }
      }
    ), true);
  }

  public void testInterfaceTest1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

      /**
       * IFoo is an interface
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {
          final String typeStr = "IFoo";

          final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

          final IClass iClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, iClass);

          Assert.assertTrue("Expected IFoo to be an interface.", iClass.isInterface());
        }
      },

      /**
       * Foo implements IFoo
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {
          final String typeStr = "Foo";

          final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

          final IClass iClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, iClass);

          final Collection<IClass> interfaces = iClass.getDirectInterfaces();

          Assert.assertEquals("Expected one single interface.", interfaces.size(), 1);

          Assert.assertTrue("Expected Foo to implement IFoo", interfaces.contains(cg.getClassHierarchy().lookupClass(
            findOrCreateTypeReference("Source", "IFoo", cg.getClassHierarchy()))));
        }
      }), true);
  }

  public void testInheritance1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
      /**
       * 'Derived' extends 'Base'
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {
          final String typeStr = "Derived";

          final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

          final IClass derivedClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, derivedClass);

          final TypeReference baseType = findOrCreateTypeReference("Source", "Base", cg.getClassHierarchy());
          final IClass baseClass = cg.getClassHierarchy().lookupClass(baseType);

          Assert.assertTrue("Expected 'Base' to be the superclass of 'Derived'", derivedClass.getSuperclass().equals(baseClass));

          Collection<IClass> subclasses = cg.getClassHierarchy().computeSubClasses(baseType);

          Assert.assertTrue("Expected subclasses of 'Base' to be 'Base' and 'Derived'.",
            subclasses.contains(derivedClass) && subclasses.contains(baseClass));
        }
      }), true);
  }

  public void testArray1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
      /**
       * 'foo' has four array instructions:
       * - 2 SSAArrayLengthInstruction
       * - 1 SSAArrayLoadInstruction
       * - 1 SSAArrayStoreInstruction
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {

          MethodReference mref = descriptorToMethodRef("Source#Array1#foo#()V", cg.getClassHierarchy());

          int count = 0;
          CGNode node = cg.getNodes(mref).iterator().next();
          for(SSAInstruction s : node.getIR().getInstructions()) {
            if(isArrayInstruction(s)) {
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

  public void testArrayLiteral1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
      /**
       * 'foo' has four array instructions:
       * - 2 SSAArrayLengthInstruction
       * - 1 SSAArrayLoadInstruction
       * - 1 SSAArrayStoreInstruction
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {

          MethodReference mref = descriptorToMethodRef("Source#ArrayLiteral1#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

          CGNode node = cg.getNodes(mref).iterator().next();
          SSAInstruction s = node.getIR().getInstructions()[3];
          Assert.assertTrue("Did not find new array instruction.", s instanceof SSANewInstruction);
          Assert.assertTrue("", ((SSANewInstruction)s).getNewSite().getDeclaredType().isArrayType());
        }

      }), true);
  }

  public void testArrayLiteral2() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
      /**
       * int[] y= { 1, 2, 3, 4 } is represented in the IR as four array store instructions
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {

          MethodReference mref = descriptorToMethodRef("Source#ArrayLiteral2#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

          CGNode node = cg.getNodes(mref).iterator().next();

          final SSAInstruction[] instructions = node.getIR().getInstructions();
          // test 1
          {
            SSAInstruction s1 = instructions[3];
            if(s1 instanceof SSANewInstruction) {
              Assert.assertTrue("", ((SSANewInstruction)s1).getNewSite().getDeclaredType().isArrayType());
            } else {
              Assert.assertTrue("Expected 3rd to be a new array instruction.", false);
            }
          }
          // test 2
          {
            SSAInstruction s2 = instructions[4];
            if(s2 instanceof SSANewInstruction) {
              Assert.assertTrue("", ((SSANewInstruction)s2).getNewSite().getDeclaredType().isArrayType());
            } else {
              Assert.assertTrue("Expected 4th to be a new array instruction.", false);
            }
          }
          // test 3: the last 4 instructions are of the form y[i] = i+1;
          {
            final SymbolTable symbolTable = node.getIR().getSymbolTable();
            for(int i = 5; i <= 8; i++) {
              Assert.assertTrue("Expected only array stores.", instructions[i] instanceof SSAArrayStoreInstruction);

              SSAArrayStoreInstruction as = (SSAArrayStoreInstruction)instructions[i];

              Assert.assertEquals("Expected an array store to 'y'.", node.getIR().getLocalNames(i, as.getArrayRef())[0], "y");

              final Integer valueOfArrayIndex = ((Integer)symbolTable.getConstantValue(as.getIndex()));
              final Integer valueAssigned = (Integer)symbolTable.getConstantValue(as.getValue());

              Assert.assertEquals("Expected an array store to 'y' with value " + (valueOfArrayIndex + 1),
                valueAssigned.intValue(), valueOfArrayIndex + 1);

            }
          }
        }

      }), true);
  }

  public void testInheritedField() {
    List<EdgeAssertions> edgeAssertionses = Arrays.asList(
      EdgeAssertions.make("Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#foo#()V"),
      EdgeAssertions.make("Source#InheritedField#main#([Ljava/lang/String;)V", "Source#B#bar#()V"));
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), edgeAssertionses, true);
  }

  public void testQualifiedStatic() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(
      /**
       *
       */
      new IRAssertion() {

        public void check(CallGraph cg) throws Exception {

          MethodReference mref = descriptorToMethodRef("Source#QualifiedStatic#main#([Ljava/lang/String;)V", cg.getClassHierarchy());

          CGNode node = cg.getNodes(mref).iterator().next();
          SSAInstruction s = node.getIR().getInstructions()[5];

          Assert.assertTrue("Did not find a getstatic instruction.", s instanceof SSAGetInstruction && ((SSAGetInstruction)s).isStatic());
          final FieldReference field = ((SSAGetInstruction)s).getDeclaredField();
          Assert.assertEquals("Expected a getstatic for 'value'.", field.getName().toString(), "value");
          Assert.assertEquals("Expected a getstatic for 'value'.", field.getDeclaringClass().getName().toString(), "LFooQ");
        }

      }), true);
  }

  public void testStaticNesting() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

      new IRAssertion() {

        public void check(CallGraph cg)
        throws Exception {
          final String typeStr = singleInputForTestNoExt() + "$WhatsIt";

          final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

          final IClass iClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, iClass);

          //todo: this fails: Assert.assertNotNull("Expected to be enclosed in 'StaticNesting'.", ((JavaSourceLoaderImpl.JavaClass)iClass).getEnclosingClass());
          //todo: is there the concept of CompilationUnit?

          /**
           * {@link JavaCAst2IRTranslator#getEnclosingType} return null for static inner classes..?
           */
        }
      }
    ), true);
  }

  public void testInnerClass() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

      new IRAssertion() {

        public void check(CallGraph cg)
        throws Exception {
          final String typeStr = singleInputForTestNoExt();

          final TypeReference type = findOrCreateTypeReference("Source", typeStr + "$WhatsIt", cg.getClassHierarchy());

          final IClass iClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, iClass);

          Assert.assertEquals("Expected to be enclosed in 'InnerClass'.",
            ((JavaSourceLoaderImpl.JavaClass)iClass).getEnclosingClass(), //todo is there another way?
            cg.getClassHierarchy().lookupClass(findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy())));
        }
      }
    ), true);
  }

  public void testLocalClass() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

      new IRAssertion() {

        /**
         * Classes local to method are enclosed in the class the methods belong to.
         */
        public void check(CallGraph cg)
        throws Exception {
          final String typeStr = singleInputForTestNoExt();
          final String localClassStr = "Foo";

          // Observe the descriptor for a class local to a method.
          final TypeReference mainFooType = findOrCreateTypeReference("Source",
            typeStr + "/main([Ljava/lang/String;)V/" + localClassStr, cg.getClassHierarchy());

          // Observe the descriptor for a class local to a method.
          final IClass mainFooClass = cg.getClassHierarchy().lookupClass(mainFooType);
          Assert.assertNotNull("Could not find class " + mainFooType, mainFooClass);

          final TypeReference methodFooType = findOrCreateTypeReference("Source",
            typeStr + "/method()V/" + localClassStr, cg.getClassHierarchy());

          final IClass methodFooClass = cg.getClassHierarchy().lookupClass(methodFooType);
          Assert.assertNotNull("Could not find class " + methodFooType, methodFooClass);

          final IClass localClass = cg.getClassHierarchy().lookupClass(findOrCreateTypeReference("Source",
            typeStr, cg.getClassHierarchy()));

          Assert.assertSame("'Foo' is enclosed in 'Local'", ((JavaSourceLoaderImpl.JavaClass)methodFooClass).getEnclosingClass(),
            localClass);
          //todo: is this failing because 'main' is static?
//          Assert.assertSame("'Foo' is enclosed in 'Local'", ((JavaSourceLoaderImpl.JavaClass)mainFooClass).getEnclosingClass(),
//            localClass);
        }
      }
    ), true);
  }

  public void testAnonymousClass() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), Arrays.asList(

      new IRAssertion() {

        public void check(CallGraph cg)
        throws Exception {
          final String typeStr = singleInputForTestNoExt();

          final TypeReference type = findOrCreateTypeReference("Source", typeStr, cg.getClassHierarchy());

          final IClass iClass = cg.getClassHierarchy().lookupClass(type);
          Assert.assertNotNull("Could not find class " + typeStr, iClass);

          //todo what to check?? could not find anything in the APIs for anonymous
        }
      }
    ), true);
  }

  public void testWhileTest1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testSwitch1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testException1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testException2() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testFinally1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testScoping1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testScoping2() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testNonPrimaryTopLevel() {
    runTest(singlePkgTestSrc("p"), rtJar, simplePkgTestEntryPoint("p"), emptyList, true);
  }

  public void testMiniaturList() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testMonitor() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testStaticInit() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testThread1() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  public void testCasts() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

  private MethodReference 
    getSliceRootReference(String className,
			  String methodName,
			  String methodDescriptor) 
  {
    TypeName clsName = 
      TypeName.string2TypeName("L" + className.replace('.', '/'));
    TypeReference clsRef = 
      TypeReference.findOrCreate(EclipseProjectPath.SOURCE_REF, clsName);
    
    Atom nameAtom = Atom.findOrCreateUnicodeAtom(methodName);
    Descriptor descr = Descriptor.findOrCreateUTF8(methodDescriptor);

    return MethodReference.findOrCreate(clsRef, nameAtom, descr);
  }

  public void testMiniaturSliceBug() throws IllegalArgumentException, CancelException {
    Pair x = runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);

    PointerAnalysis pa = (PointerAnalysis) x.snd;
    CallGraph cg = (CallGraph) x.fst;

    // test partial slice 
    MethodReference sliceRootRef = 
      getSliceRootReference("MiniaturSliceBug", "validNonDispatchedCall", "(LIntWrapper;)V");
    Set roots = cg.getNodes( sliceRootRef );
    Pair<Collection<Statement>, SDG>  y = AstJavaSlicer.computeAssertionSlice(cg, pa, roots);
    Collection<Statement> slice = (Collection<Statement>) y.fst;
    SlicerTest.dumpSlice(slice);
    assertEquals(0, SlicerTest.countAllocations(slice));
    assertEquals(1, SlicerTest.countPutfields(slice));

    // test slice from main
    sliceRootRef = 
      getSliceRootReference("MiniaturSliceBug", "main", "([Ljava/lang/String;)V");
    roots = cg.getNodes( sliceRootRef );
    y = AstJavaSlicer.computeAssertionSlice(cg, pa, roots);
    slice = (Collection<Statement>) y.fst;
    SlicerTest.dumpSlice(slice);
    assertEquals(2, SlicerTest.countAllocations(slice));
    assertEquals(2, SlicerTest.countPutfields(slice));
  }

}
