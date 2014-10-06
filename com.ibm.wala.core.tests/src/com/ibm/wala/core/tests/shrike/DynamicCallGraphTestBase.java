/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.core.tests.shrike;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrike.cg.DynamicCallGraph;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;

public abstract class DynamicCallGraphTestBase extends WalaTestCase {
  
  protected static String getClasspathEntry(String elt) {
    for (String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
      if (s.indexOf(elt) >= 0) {
        File e = new File(s);
         Assert.assertTrue(elt + " expected to exist", e.exists());
        if (e.isDirectory() && !s.endsWith("/")) {
          s = s + "/";
        }
        return s;
      }
    }
    Assert.assertFalse("cannot find " + elt, true);
    return null;
  }
  
  private boolean instrumentedJarBuilt = false;
  
  private static String instrumentedJarLocation = System.getProperty("java.io.tmpdir") + File.separator + "test.jar";

  private static String cgLocation = System.getProperty("java.io.tmpdir") + File.separator + "cg.txt";

  protected void instrument(String testJarLocation) throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException {
    if (! instrumentedJarBuilt) {
      System.err.println("core data jar to instrument: " + testJarLocation);
      
      if (new File(instrumentedJarLocation).exists()) {
        assert new File(instrumentedJarLocation).delete();
      }
      
      String rtJar = null;
      for(String jar : WalaProperties.getJ2SEJarFiles()) {
        if (jar.endsWith("rt.jar") || jar.endsWith("classes.jar")) {
          rtJar = jar;
        }
      }
      
      DynamicCallGraph.main(
          rtJar == null?
            new String[]{testJarLocation, "-o", instrumentedJarLocation}:
            new String[]{testJarLocation, "-o", instrumentedJarLocation, "--rt-jar", rtJar});
      Assert.assertTrue("expected to create /tmp/test.jar", new File(instrumentedJarLocation).exists());   
      instrumentedJarBuilt = true;
    }
  }
  
  protected void run(String mainClass, String exclusionsFile, String... args) throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    String shrikeBin = getClasspathEntry("com.ibm.wala.shrike");
    String utilBin = getClasspathEntry("com.ibm.wala.util");
    URLClassLoader jcl = new URLClassLoader(new URL[]{ new URL("file://" + instrumentedJarLocation), new URL("file://" + shrikeBin), new URL("file://" + utilBin) }, DynamicCallGraphTestBase.class.getClassLoader().getParent());
 
    Class<?> testClass = jcl.loadClass(mainClass);
    Assert.assertNotNull(testClass);
    Method testMain = testClass.getDeclaredMethod("main", String[].class);
    Assert.assertNotNull(testMain);

    System.setProperty("dynamicCGFile", cgLocation);
    System.setProperty("dynamicCGHandleMissing", "true");
    if (exclusionsFile != null) {
      File tmpFile = TemporaryFile.urlToFile("exclusions.txt", getClass().getClassLoader().getResource(exclusionsFile));
      System.setProperty("dynamicCGFilter", tmpFile.getCanonicalPath());
    }
    try {
      testMain.invoke(null, args==null? new Object[0]: new Object[]{args});      
    } catch (Throwable e) {
      // exceptions here are from the instrumented program
      // this is fine, since we are collecting its call graph
      // and exceptions are possible behavior.

      // well, most errors are fine.  On the other hand, low-level 
      // class loading errors likely indicate a bug in instrumentation,
      // which is often tested with this test.
      while (e.getCause() != null) {
        Assert.assertFalse(String.valueOf(e.getCause()), e.getCause() instanceof LinkageError);
        e = e.getCause();
      }
    }
    
    // the VM is not exiting, so stop tracing explicitly
    Class<?> runtimeClass = jcl.loadClass("com.ibm.wala.shrike.cg.Runtime");
    Assert.assertNotNull(runtimeClass);
    Method endTrace = runtimeClass.getDeclaredMethod("endTrace");
    Assert.assertNotNull(endTrace);
    endTrace.invoke(null);

    Assert.assertTrue("expected to create call graph", new File(System.getProperty("dynamicCGFile")).exists());
  }
   
  interface EdgesTest {
    void edgesTest(CallGraph staticCG, CGNode caller, MethodReference callee);
  }
 
  private MethodReference callee(String calleeClass, String calleeMethod) {
    return MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, "L" + calleeClass), Selector.make(calleeMethod));
  }

  protected void checkEdges(CallGraph staticCG) throws IOException {
    checkEdges(staticCG, Predicate.<MethodReference>truePred());
  }
  
  protected void checkEdges(CallGraph staticCG, Predicate<MethodReference> filter) throws IOException {
    check(staticCG, new EdgesTest() {
      private final Set<Pair<CGNode,CGNode>> edges = HashSetFactory.make();
      @Override
      public void edgesTest(CallGraph staticCG, CGNode caller, MethodReference calleeRef) {
        if (! calleeRef.getName().equals(MethodReference.clinitName)) {
          Set<CGNode> nodes = staticCG.getNodes(calleeRef);
          Assert.assertEquals(1, nodes.size());
          CGNode callee = nodes.iterator().next();
        
          Assert.assertTrue("no edge for " + caller + " --> " + callee, staticCG.getPossibleSites(caller, callee).hasNext());
          Pair<CGNode,CGNode> x = Pair.make(caller, callee);
          if (! edges.contains(x)) {
            edges.add(x);
            System.err.println("found expected edge" + caller + " --> " + callee);
          }
        }
      }
    }, filter);
  }
 
  protected void checkNodes(CallGraph staticCG) throws IOException {
    checkNodes(staticCG, Predicate.<MethodReference>truePred());
  }

  protected void checkNodes(CallGraph staticCG, Predicate<MethodReference> filter) throws IOException {
    final Set<MethodReference> notFound = HashSetFactory.make();
    check(staticCG, new EdgesTest() {
      @Override
      public void edgesTest(CallGraph staticCG, CGNode caller, MethodReference callee) {
        boolean checkForCallee = !staticCG.getNodes(callee).isEmpty();
        if (!checkForCallee) {
          notFound.add(callee);
        } else {
          System.err.println("found expected node " + callee);
        }
      }
    }, filter);
    
    Assert.assertTrue("could not find " + notFound, notFound.isEmpty());
  }
 
  protected void check(CallGraph staticCG, EdgesTest test, Predicate<MethodReference> filter) throws IOException {
    BufferedReader dynamicEdgesFile = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(System.getProperty("dynamicCGFile")))));
    String line;
    int lines = 0;
    loop: while ((line = dynamicEdgesFile.readLine()) != null) {
      lines++;
      StringTokenizer edge = new StringTokenizer(line, "\t");
      
      CGNode caller;
      String callerClass = edge.nextToken();
      if ("root".equals(callerClass)) {
        caller = staticCG.getFakeRootNode();
      } else {
        String callerMethod = edge.nextToken();
        MethodReference callerRef = MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, "L" + callerClass), Selector.make(callerMethod));
        Set<CGNode> nodes = staticCG.getNodes(callerRef);
        if (! filter.test(callerRef)) {
          continue loop;
        }
        Assert.assertEquals(1, nodes.size());
        caller = nodes.iterator().next();
      }
      
      String calleeClass = edge.nextToken();
      String calleeMethod = edge.nextToken();
      MethodReference callee = callee(calleeClass, calleeMethod);
      if (! filter.test(callee)) {
        continue loop;
      }
      test.edgesTest(staticCG, caller, callee);
    }
    
    dynamicEdgesFile.close();
    
    Assert.assertTrue("more than one edge", lines > 0);
  }
  
}
