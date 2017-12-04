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
 * Created on Oct 3, 2005
 */
package com.ibm.wala.cast.java.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import org.junit.Assert;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public abstract class IRTests {

  protected boolean dump = false;
  
  protected IRTests(String projectName) {
    this.projectName = projectName;
  }

  protected final String projectName;

  protected static String javaHomePath;

  private String testSrcPath = "." + File.separator + "src";

  public static final List<String> rtJar = Arrays.asList(WalaProperties.getJ2SEJarFiles());

  protected static List<IRAssertion> emptyList = Collections.emptyList();

  // TODO delete this code; leaving just in case --MS
//  static {
//    boolean found = false;
//    try {
//      rtJar = new LinkedList<String>();
//
//      Properties p = WalaProperties.loadProperties();
//      javaHomePath = p.getProperty(WalaProperties.J2SE_DIR);
//
//      if (new File(javaHomePath).isDirectory()) {
//        if ("Mac OS X".equals(System.getProperty("os.name"))) { // nick
//          /**
//           * todo: {@link WalaProperties#getJ2SEJarFiles()}
//           */
//          rtJar.add(javaHomePath + "/classes.jar");
//          rtJar.add(javaHomePath + "/ui.jar");
//        } else {
//          rtJar.add(javaHomePath + File.separator + "classes.jar");
//          rtJar.add(javaHomePath + File.separator + "rt.jar");
//          rtJar.add(javaHomePath + File.separator + "core.jar");
//          rtJar.add(javaHomePath + File.separator + "vm.jar");
//        }
//        found = true;
//      }
//    } catch (Exception e) {
//      // no properties
//    }
//
//    if (!found) {
//      javaHomePath = System.getProperty("java.home");
//      if ("Mac OS X".equals(System.getProperty("os.name"))) { // nick
//        rtJar.add(javaHomePath + "/../Classes/classes.jar");
//        rtJar.add(javaHomePath + "/../Classes/ui.jar");
//      } else {
//        rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "rt.jar");
//        rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "core.jar");
//        rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "vm.jar");
//        rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "classes.jar");
//      }
//    }
//  }

  public interface IRAssertion {

    void check(CallGraph cg);

  }

  protected static class EdgeAssertions implements IRAssertion {
    public final String srcDescriptor;

    public final List/* <String> */<String> tgtDescriptors = new ArrayList<>();

    public EdgeAssertions(String srcDescriptor) {
      this.srcDescriptor = srcDescriptor;
    }

    public static EdgeAssertions make(String srcDescriptor, String tgtDescriptor) {
      EdgeAssertions ea = new EdgeAssertions(srcDescriptor);
      ea.tgtDescriptors.add(tgtDescriptor);
      return ea;
    }

    public static EdgeAssertions make(String srcDescriptor, String tgtDescriptor1, String tgtDescriptor2) {
      EdgeAssertions ea = new EdgeAssertions(srcDescriptor);
      ea.tgtDescriptors.add(tgtDescriptor1);
      ea.tgtDescriptors.add(tgtDescriptor2);
      return ea;
    }

    public static EdgeAssertions make(String srcDescriptor, String tgtDescriptor1, String tgtDescriptor2, String tgtDescriptor3) {
      EdgeAssertions ea = new EdgeAssertions(srcDescriptor);
      ea.tgtDescriptors.add(tgtDescriptor1);
      ea.tgtDescriptors.add(tgtDescriptor2);
      ea.tgtDescriptors.add(tgtDescriptor3);
      return ea;
    }

    public static EdgeAssertions make(String srcDescriptor, String tgtDescriptor1, String tgtDescriptor2, String tgtDescriptor3,
        String tgtDescriptor4) {
      EdgeAssertions ea = new EdgeAssertions(srcDescriptor);
      ea.tgtDescriptors.add(tgtDescriptor1);
      ea.tgtDescriptors.add(tgtDescriptor2);
      ea.tgtDescriptors.add(tgtDescriptor3);
      ea.tgtDescriptors.add(tgtDescriptor4);
      return ea;
    }

    @Override
    public void check(CallGraph callGraph) {
      MethodReference srcMethod = descriptorToMethodRef(this.srcDescriptor, callGraph.getClassHierarchy());
      Set<CGNode> srcNodes = callGraph.getNodes(srcMethod);

      if (srcNodes.size() == 0) {
        System.err.println(("Unreachable/non-existent method: " + srcMethod));
        return;
      }
      if (srcNodes.size() > 1) {
        System.err.println("Context-sensitive call graph?");
      }

      // Assume only one node for src method
      CGNode srcNode = srcNodes.iterator().next();

      for (String target : this.tgtDescriptors) {
        MethodReference tgtMethod = descriptorToMethodRef(target, callGraph.getClassHierarchy());
        // Assume only one node for target method
        Set<CGNode> tgtNodes = callGraph.getNodes(tgtMethod);
        if (tgtNodes.size() == 0) {
          System.err.println(("Unreachable/non-existent method: " + tgtMethod));
          continue;
        }
        CGNode tgtNode = tgtNodes.iterator().next();

        boolean found = false;
        for (CGNode succ : Iterator2Iterable.make(callGraph.getSuccNodes(srcNode))) {
          if (tgtNode == succ) {
            found = true;
            break;
          }
        }
        if (!found) {
          System.err.println(("Missing edge: " + srcMethod + " -> " + tgtMethod));
        }
      }
    }
  }

  protected static class SourceMapAssertion implements IRAssertion {
    private final String method;

    private final String variableName;

    private final int definingLineNumber;

    protected SourceMapAssertion(String method, String variableName, int definingLineNumber) {
      this.method = method;
      this.variableName = variableName;
      this.definingLineNumber = definingLineNumber;
    }

    @Override
    public void check(CallGraph cg) {

      MethodReference mref = descriptorToMethodRef(method, cg.getClassHierarchy());

      for (CGNode cgNode : cg.getNodes(mref)) {
        Assert.assertTrue("failed for " + this.variableName + " in " + cgNode + "\n" + cgNode.getIR(), this.check(cgNode.getMethod(), cgNode.getIR()));
      }
    }

    boolean check(IMethod m, IR ir) {
      System.err.println(("check for " + variableName + " defined at " + definingLineNumber));
      SSAInstruction[] insts = ir.getInstructions();
      for (int i = 0; i < insts.length; i++) {
        if (insts[i] != null) {
          int ln = m.getLineNumber(i);
          if (ln == definingLineNumber) {
            System.err.println(("  found " + insts[i] + " at " + ln));
            for (int j = 0; j < insts[i].getNumberOfDefs(); j++) {
              int def = insts[i].getDef(j);
              System.err.println(("    looking at def " + j + ": " + def));
              String[] names = ir.getLocalNames(i, def);
              if (names != null) {
                for (String name : names) {
                  System.err.println(("      looking at name " + name));
                  if (name.equals(variableName)) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }

      return false;
    }
  }

  protected static class AnnotationAssertions implements IRAssertion {

    public static class ClassAnnotation {
      private final String className;
      private final String annotationTypeName;
      
      public ClassAnnotation(String className, String annotationTypeName) {
        super();
        this.className = className;
        this.annotationTypeName = annotationTypeName;
      }
    }

    public static class MethodAnnotation {
      private final String methodSig;
      private final String annotationTypeName;
      
      public MethodAnnotation(String methodSig, String annotationTypeName) {
        super();
        this.methodSig = methodSig;
        this.annotationTypeName = annotationTypeName;
      }
    }

    public final Set<ClassAnnotation> classAnnotations = HashSetFactory.make();
    public final Set<MethodAnnotation> methodAnnotations = HashSetFactory.make();
    
    @Override
    public void check(CallGraph cg) {
      classes: for(ClassAnnotation ca : classAnnotations) {
        IClass cls = cg.getClassHierarchy().lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, ca.className));
        IClass at = cg.getClassHierarchy().lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, ca.annotationTypeName));
        for(Annotation a : cls.getAnnotations()) {
          if (a.getType().equals(at.getReference())) {
            continue classes;
          }
        }
        
        Assert.assertFalse("cannot find " + at + " in " + cls, false);
      }
    
      annot: for(MethodAnnotation ma : methodAnnotations) {
        IClass at = cg.getClassHierarchy().lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, ma.annotationTypeName));
        for(CGNode n : cg) {
          if (n.getMethod().getSignature().equals(ma.methodSig)) {
            for(Annotation a : n.getMethod().getAnnotations()) {
              if (a.getType().equals(at.getReference())) {
                continue annot;
              }
            }
          
            Assert.assertFalse("cannot find " + at, false);
          }
        }
      }
    }
  }
  
  protected Collection<String> singleTestSrc() {
    return Collections.singletonList(getTestSrcPath() + File.separator + singleJavaInputForTest());
  }

  protected Collection<String> singleTestSrc(final String folder) {
    return Collections.singletonList(getTestSrcPath() + File.separator + folder + File.separator + singleJavaInputForTest());
  }

  protected Collection<String> singlePkgTestSrc(String pkgName) {
    return Collections.singletonList(getTestSrcPath() + File.separator + singleJavaPkgInputForTest(pkgName));
  }

  protected String getTestName() {
    StackTraceElement stack[] = new Throwable().getStackTrace();
    for(int i = 0; i <= stack.length; i++) {
      if (stack[i].getMethodName().startsWith("test")) {
        return stack[i].getMethodName();    
      }
    }
    
    throw new Error("test method not found");
  }
  
  protected String[] simpleTestEntryPoint() {
    return new String[] { "L" + getTestName().substring(4) };
  }

  protected String[] simplePkgTestEntryPoint(String pkgName) {
    return new String[] { "L" + pkgName + "/" + getTestName().substring(4) };
  }

  protected abstract <I extends InstanceKey> AbstractAnalysisEngine<I> getAnalysisEngine(String[] mainClassDescriptors, Collection<String> sources, List<String> libs);

  public <I extends InstanceKey> Pair<CallGraph, PointerAnalysis<I>> runTest(Collection<String> sources, List<String> libs,
        String[] mainClassDescriptors, List<? extends IRAssertion> ca, boolean assertReachable) throws IllegalArgumentException, CancelException, IOException {
      AbstractAnalysisEngine<I> engine = getAnalysisEngine(mainClassDescriptors, sources, libs);

      CallGraph callGraph;
        callGraph = engine.buildDefaultCallGraph();
        //System.err.println(callGraph.toString());

        // If we've gotten this far, IR has been produced.
        if (dump) {
          dumpIR(callGraph, sources, assertReachable);
        }
        
        // Now check any assertions as to source mapping
        for (IRAssertion IRAssertion : ca) {
          IRAssertion.check(callGraph);
        }

        return Pair.make(callGraph, engine.getPointerAnalysis());
  }

  protected static void dumpIR(CallGraph cg, Collection<String> sources, boolean assertReachable) {
    Set<String> sourcePaths = HashSetFactory.make();
    for(String src : sources) {
      sourcePaths.add(src.substring(src.lastIndexOf(File.separator)+1));
    }
    
    Set<IMethod> unreachable = HashSetFactory.make();
    IClassHierarchy cha = cg.getClassHierarchy();
    IClassLoader sourceLoader = cha.getLoader(JavaSourceAnalysisScope.SOURCE);
    for (IClass clazz : Iterator2Iterable.make(sourceLoader.iterateAllClasses())) {

      System.err.println(clazz);
      if (clazz.isInterface())
        continue;

      for (IMethod m : clazz.getDeclaredMethods()) {
        if (m.isAbstract()) {
          System.err.println(m);
        } else {
          Iterator<CGNode> nodeIter = cg.getNodes(m.getReference()).iterator();
          if (!nodeIter.hasNext()) {
            if (m instanceof AstMethod) {
              String fn = ((AstClass)m.getDeclaringClass()).getSourcePosition().getURL().getFile();
              if (sourcePaths.contains(fn.substring(fn.lastIndexOf(File.separator)+1))) {
                System.err.println(("Method " + m.getReference() + " not reachable?"));
                unreachable.add(m);
              }
            }
            continue;
          }
          CGNode node = nodeIter.next();
          System.err.println(node.getIR());
        }
      }
    }

    if (assertReachable) {
      Assert.assertTrue("unreachable methods: " + unreachable.toString(), unreachable.isEmpty());
    }
  }

  /**
   * 
   * @param srcMethodDescriptor a full method descriptor of the form ldr#type#methName#methSig example:
   *          Source#Simple1#main#([Ljava/lang/String;)V
   * @param cha
   */
  public static MethodReference descriptorToMethodRef(String srcMethodDescriptor, IClassHierarchy cha) {
    String[] ldrTypeMeth = srcMethodDescriptor.split("\\#");

    String loaderName = ldrTypeMeth[0];
    String typeStr = ldrTypeMeth[1];
    String methName = ldrTypeMeth[2];
    String methSig = ldrTypeMeth[3];

    TypeReference typeRef = findOrCreateTypeReference(loaderName, typeStr, cha);

    Language l = cha.getLoader(typeRef.getClassLoader()).getLanguage();
    return MethodReference.findOrCreate(l, typeRef, methName, methSig);
  }

  static TypeReference findOrCreateTypeReference(String loaderName, String typeStr, IClassHierarchy cha) {
    ClassLoaderReference clr = findLoader(loaderName, cha);
    TypeName typeName = TypeName.string2TypeName("L" + typeStr);
    TypeReference typeRef = TypeReference.findOrCreate(clr, typeName);
    return typeRef;
  }

  private static ClassLoaderReference findLoader(String loaderName, IClassHierarchy cha) {
    Atom loaderAtom = Atom.findOrCreateUnicodeAtom(loaderName);
    IClassLoader[] loaders = cha.getLoaders();
    for (IClassLoader loader : loaders) {
      if (loader.getName() == loaderAtom) {
        return loader.getReference();
      }
    }
    Assertions.UNREACHABLE();
    return null;
  }

  public static void populateScope(JavaSourceAnalysisEngine<?> engine, Collection<String> sources, List<String> libs) {
    boolean foundLib = false;
    for (String lib : libs) {
      File libFile = new File(lib);
      if (libFile.exists()) {
        foundLib = true;
        try {
          engine.addSystemModule(new JarFileModule(new JarFile(libFile, false)));
        } catch (IOException e) {
          Assert.fail(e.getMessage());
        }
      }
    }
    assert foundLib : "couldn't find library file from " + libs;

    for (String srcFilePath : sources) {
      String srcFileName = srcFilePath.substring(srcFilePath.lastIndexOf(File.separator) + 1);
      File f = new File(srcFilePath);
      Assert.assertTrue("couldn't find " + srcFilePath, f.exists());
      if (f.isDirectory()) {
        engine.addSourceModule(new SourceDirectoryTreeModule(f));
      } else {
        engine.addSourceModule(new SourceFileModule(f, srcFileName, null));
      }
    }
  }

  protected void setTestSrcPath(String testSrcPath) {
    this.testSrcPath = testSrcPath;
  }

  protected String getTestSrcPath() {
    return testSrcPath;
  }

  protected String singleJavaInputForTest() {
    return getTestName().substring(4) + ".java";
  }

  protected String singleInputForTest() {
    return getTestName().substring(4);
  }

  protected String singleJavaPkgInputForTest(String pkgName) {
    return pkgName + File.separator + getTestName().substring(4) + ".java";
  }

}
