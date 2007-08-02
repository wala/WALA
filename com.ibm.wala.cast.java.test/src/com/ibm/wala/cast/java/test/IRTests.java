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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;

import junit.framework.Assert;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.*;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.*;
import com.ibm.wala.util.warnings.WalaException;

public abstract class IRTests extends WalaTestCase {
  public IRTests(String name) {
    super(name);
  }

  protected static String javaHomePath = System.getProperty("java.home");

  protected static String testSrcPath = "." + File.separator + "testSrc";

  protected static List<String> rtJar;

  static {
    rtJar = new LinkedList<String>();
    rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "rt.jar");
    rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "core.jar");
    rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "vm.jar");
    rtJar.add(javaHomePath + File.separator + "lib" + File.separator + "classes.jar");
    try {
      Properties p = WalaProperties.loadProperties();
      String javaHomePath = p.getProperty(WalaProperties.J2SE_DIR);
      rtJar.add(javaHomePath + File.separator + "classes.jar");
      rtJar.add(javaHomePath + File.separator + "rt.jar");
      rtJar.add(javaHomePath + File.separator + "core.jar");
      rtJar.add(javaHomePath + File.separator + "vm.jar");    
    } catch (WalaException e) {
      // no properties
    }

  }

  protected static class EdgeAssertions {
    public final String srcDescriptor;

    public final List/* <String> */<String> tgtDescriptors = new ArrayList<String>();

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
  }

  protected static class GraphAssertions {
    public final Set/* <EdgeAssertions> */<EdgeAssertions> nodeAssertions = HashSetFactory.make();

    public GraphAssertions() {
    }

    public GraphAssertions(EdgeAssertions ea1) {
      nodeAssertions.add(ea1);
    }

    public GraphAssertions(EdgeAssertions ea1, EdgeAssertions ea2) {
      nodeAssertions.add(ea1);
      nodeAssertions.add(ea2);
    }

    public GraphAssertions(EdgeAssertions ea1, EdgeAssertions ea2, EdgeAssertions ea3) {
      nodeAssertions.add(ea1);
      nodeAssertions.add(ea2);
      nodeAssertions.add(ea3);
    }

    public GraphAssertions(EdgeAssertions ea1, EdgeAssertions ea2, EdgeAssertions ea3, EdgeAssertions ea4) {
      nodeAssertions.add(ea1);
      nodeAssertions.add(ea2);
      nodeAssertions.add(ea3);
      nodeAssertions.add(ea4);
    }
  }

  protected static class SourceMapAssertion {
    private final String variableName;

    private final int definingLineNumber;

    protected SourceMapAssertion(String variableName, int definingLineNumber) {
      this.variableName = variableName;
      this.definingLineNumber = definingLineNumber;
    }

    boolean check(IMethod m, IR ir) {
      Trace.println("check for " + variableName + " defined at " + definingLineNumber);
      SSAInstruction[] insts = ir.getInstructions();
      for (int i = 0; i < insts.length; i++) {
        if (insts[i] != null) {
          int ln = -1;
          try {
            ln = m.getLineNumber(i);
          } catch (InvalidClassFileException e) {
            e.printStackTrace();
            Assertions.UNREACHABLE();
          }
          if (ln == definingLineNumber) {
            Trace.println("  found " + insts[i] + " at " + ln);
            for (int j = 0; j < insts[i].getNumberOfDefs(); j++) {
              int def = insts[i].getDef(j);
              Trace.println("    looking at def " + j + ": " + def);
              String[] names = ir.getLocalNames(i, def);
              if (names != null) {
                for (int n = 0; n < names.length; n++) {
                  Trace.println("      looking at name " + names[n]);
                  if (names[n].equals(variableName)) {
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

  protected static class SourceMapAssertions {

    private final Map<String, Set<SourceMapAssertion>> methodAssertions = HashMapFactory.make();

    protected void addAssertion(String method, SourceMapAssertion a) {
      Set<SourceMapAssertion> x = MapUtil.findOrCreateSet(methodAssertions, method);
      x.add(a);
    }

    void check(CallGraph CG) {
      for (Iterator ms = methodAssertions.entrySet().iterator(); ms.hasNext();) {
        Map.Entry entry = (Map.Entry) ms.next();

        Set s = (Set) entry.getValue();

        String method = (String) entry.getKey();
        MethodReference mref = descriptorToMethodRef(method, CG.getClassHierarchy());

        for (Iterator ns = CG.getNodes(mref).iterator(); ns.hasNext();) {
          CGNode n = (CGNode) ns.next();
          for (Iterator as = s.iterator(); as.hasNext();) {
            SourceMapAssertion a = (SourceMapAssertion) as.next();
            Assert.assertTrue("failed for " + a.variableName + " in " + n, a.check(n.getMethod(), n.getIR()));
          }
        }
      }
    }
  }

  protected abstract String singleInputForTest();

  protected abstract String singlePkgInputForTest(String pkgName);

  protected Collection singleTestSrc() {
    return Collections.singletonList(testSrcPath + File.separator + singleInputForTest());
  }

  protected Collection singlePkgTestSrc(String pkgName) {
    return Collections.singletonList(testSrcPath + File.separator + singlePkgInputForTest(pkgName));
  }

  protected String[] simpleTestEntryPoint() {
    return new String[] { "L" + getName().substring(4) };
  }

  protected String[] simplePkgTestEntryPoint(String pkgName) {
    return new String[] { "L" + pkgName + "/" + getName().substring(4) };
  }

  protected abstract JavaSourceAnalysisEngine getAnalysisEngine(String[] mainClassDescriptors);

  public void runTest(Collection/* <String> */sources, List/* <String> */libs, String[] mainClassDescriptors, GraphAssertions ga,
      SourceMapAssertions sa, boolean assertReachable) {
    try {
      JavaSourceAnalysisEngine engine = getAnalysisEngine(mainClassDescriptors);

      populateScope(engine, sources, libs);

      CallGraph callGraph = engine.buildDefaultCallGraph();
      Trace.println(callGraph.toString());

      // If we've gotten this far, IR has been produced.
      dumpIR(callGraph, assertReachable);

      // Now check any assertions as to source mapping
      if (sa != null) {
        sa.check(callGraph);
      }

      // Now check any assertions as to call-graph shape.
      checkCallGraphShape(callGraph, ga);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void dumpIR(CallGraph cg, boolean assertReachable) throws IOException {
    Set<IMethod> unreachable = HashSetFactory.make();
    IClassHierarchy cha = cg.getClassHierarchy();
    IClassLoader sourceLoader = cha.getLoader(EclipseProjectPath.SOURCE_REF);
    for (Iterator iter = sourceLoader.iterateAllClasses(); iter.hasNext();) {
      IClass clazz = (IClass) iter.next();

      Trace.println(clazz);
      if (clazz.isInterface())
        continue;

      for (Iterator iterator = clazz.getDeclaredMethods().iterator(); iterator.hasNext();) {
        IMethod m = (IMethod) iterator.next();
        if (m.isAbstract())
          Trace.println(m);
        else {
          Iterator nodeIter = cg.getNodes(m.getReference()).iterator();
          if (!nodeIter.hasNext()) {
            Trace.println("Method " + m.getReference() + " not reachable?");
            unreachable.add(m);
            continue;
          }
          CGNode node = (CGNode) nodeIter.next();
          Trace.println(node.getIR());
        }
      }
    }

    if (assertReachable) {
      Assert.assertTrue(unreachable.toString(), unreachable.isEmpty());
    }
  }

  private static void checkCallGraphShape(CallGraph callGraph, GraphAssertions ga) throws IOException {
    for (Iterator<EdgeAssertions> nodeIter = ga.nodeAssertions.iterator(); nodeIter.hasNext();) {
      EdgeAssertions ea = nodeIter.next();

      MethodReference srcMethod = descriptorToMethodRef(ea.srcDescriptor, callGraph.getClassHierarchy());
      Set/* <CGNode> */srcNodes = callGraph.getNodes(srcMethod);

      if (srcNodes.size() == 0) {
        Trace.println("Unreachable/non-existent method: " + srcMethod);
        continue;
      }
      if (srcNodes.size() > 1) {
        Trace.println("Context-sensitive call graph?");
      }

      // Assume only one node for src method
      CGNode srcNode = (CGNode) srcNodes.iterator().next();

      for (Iterator<String> edgeIter = ea.tgtDescriptors.iterator(); edgeIter.hasNext();) {
        String target = edgeIter.next();
        MethodReference tgtMethod = descriptorToMethodRef(target, callGraph.getClassHierarchy());
        // Assume only one node for target method
        Set tgtNodes = callGraph.getNodes(tgtMethod);
        if (tgtNodes.size() == 0) {
          Trace.println("Unreachable/non-existent method: " + tgtMethod);
          continue;
        }
        CGNode tgtNode = (CGNode) tgtNodes.iterator().next();

        boolean found = false;
        for (Iterator succIter = callGraph.getSuccNodes(srcNode); succIter.hasNext();) {
          CGNode succ = (CGNode) succIter.next();

          if (tgtNode == succ) {
            found = true;
            break;
          }
        }
        if (!found)
          Trace.println("Missing edge: " + srcMethod + " -> " + tgtMethod);
      }
    }
  }

  private static MethodReference descriptorToMethodRef(String descrip, IClassHierarchy cha) {
    String srcDescriptor = descrip; // ldr#type#methName#methSig
    String[] ldrTypeMeth = srcDescriptor.split("\\#");

    String loaderName = ldrTypeMeth[0];
    String typeStr = ldrTypeMeth[1];
    String methName = ldrTypeMeth[2];
    String methSig = ldrTypeMeth[3];

    ClassLoaderReference clr = findLoader(loaderName, cha);
    TypeName typeName = TypeName.string2TypeName("L" + typeStr);
    TypeReference typeRef = TypeReference.findOrCreate(clr, typeName);
    MethodReference methodRef = MethodReference.findOrCreate(typeRef, methName, methSig);

    return methodRef;
  }

  private static ClassLoaderReference findLoader(String loaderName, IClassHierarchy cha) {
    Atom loaderAtom = Atom.findOrCreateUnicodeAtom(loaderName);
    IClassLoader[] loaders = cha.getLoaders();
    for (int i = 0; i < loaders.length; i++) {
      if (loaders[i].getName() == loaderAtom)
        return loaders[i].getReference();
    }
    Assertions.UNREACHABLE();
    return null;
  }

  private static void populateScope(JavaSourceAnalysisEngine engine, Collection/* <String> */sources, List/* <String> */libs)
      throws IOException {

    boolean foundLib = false;
    for (Iterator iter = libs.iterator(); iter.hasNext();) {
      String lib = (String) iter.next();
      File libFile = new File(lib);
      if (libFile.exists()) {
        foundLib = true;
        engine.addSystemModule(new JarFileModule(new JarFile(libFile)));
      }
    }
    Assertions._assert(foundLib);

    for (Iterator iter = sources.iterator(); iter.hasNext();) {
      String srcFilePath = (String) iter.next();
      String srcFileName = srcFilePath.substring(srcFilePath.lastIndexOf(File.separator) + 1);
      File f = new File(srcFilePath);
      Assert.assertTrue(f.exists());
      if (f.isDirectory()) {
        engine.addSourceModule(new SourceDirectoryTreeModule(f));
      } else {
        engine.addSourceModule(new SourceFileModule(f, srcFileName));
      }
    }
  }
}
