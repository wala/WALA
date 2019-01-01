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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.junit.Assert;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrike.cg.OfflineDynamicCallGraph;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;

public abstract class DynamicCallGraphTestBase extends WalaTestCase {
  
  protected boolean testPatchCalls = false;
  
  private boolean instrumentedJarBuilt = false;
  
  private java.nio.file.Path instrumentedJarLocation;

  private java.nio.file.Path cgLocation;

  protected DynamicCallGraphTestBase() {
      try {
          instrumentedJarLocation = Files.createTempFile("wala-test", ".jar");
	  instrumentedJarLocation.toFile().deleteOnExit();
          cgLocation = Files.createTempFile("cg", ".txt");
	  cgLocation.toFile().deleteOnExit();
      } catch (IOException problem) {
          throw new RuntimeException(problem);
      }
  }

  protected void instrument(String testJarLocation) throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException {
    if (! instrumentedJarBuilt) {
      System.err.println("core data jar to instrument: " + testJarLocation);
      
      Files.deleteIfExists(instrumentedJarLocation);
      
      String rtJar = null;
      for(String jar : WalaProperties.getJ2SEJarFiles()) {
        if (jar.endsWith(File.separator + "rt.jar") || jar.endsWith(File.separator + "classes.jar")) {
          rtJar = jar;
        }
      }
      
      List<String> args = new ArrayList<>();
      args.addAll(Arrays.asList(testJarLocation, "-o", instrumentedJarLocation.toString()));
      if (rtJar != null) {
        args.addAll(Arrays.asList("--rt-jar", rtJar));
      }
      if (testPatchCalls) {
        args.add("--patch-calls");
      }
      OfflineDynamicCallGraph.main(args.toArray(new String[ args.size() ]));
      Assert.assertTrue("expected to create " + instrumentedJarLocation, Files.exists(instrumentedJarLocation));
      instrumentedJarBuilt = true;
    }
  }
  
  protected void run(String mainClass, String exclusionsFile, String... args) throws IOException, SecurityException, IllegalArgumentException, InterruptedException {
    Project p = new Project();
    final File projectDir = Files.createTempDirectory("wala-test").toFile();
    projectDir.deleteOnExit();
    p.setBaseDir(projectDir);
    p.init();
    p.fireBuildStarted();

    Java childJvm = new Java();
    childJvm.setTaskName("test_" + mainClass.replace('.', '_'));
    childJvm.setClasspath(new Path(p, getClasspathEntry("com.ibm.wala.shrike") + ":" +  getClasspathEntry("com.ibm.wala.util") + ":" +  instrumentedJarLocation));
    childJvm.setClassname(mainClass);

    String jvmArgs = "-noverify -Xmx500M -DdynamicCGFile=" + cgLocation + " -DdynamicCGHandleMissing=true";
    if (exclusionsFile != null) {
      File tmpFile = TemporaryFile.urlToFile("exclusions.txt", getClass().getClassLoader().getResource(exclusionsFile));
      jvmArgs += " -DdynamicCGFilter=" + tmpFile.getCanonicalPath();
    }
    childJvm.setJvmargs(jvmArgs);
    
    StringBuilder argsStr = new StringBuilder();
    for(String a : args) {
      argsStr.append(a).append(' ');
    }
    childJvm.setArgs(argsStr.toString());
    
    childJvm.setFailonerror(true);
    childJvm.setFork(true);
    
    Files.deleteIfExists(cgLocation);
    
    childJvm.init();
    String commandLine = childJvm.getCommandLine().toString();
    System.err.println(commandLine);
    Process x = Runtime.getRuntime().exec(commandLine);
    x.waitFor();
    
    Assert.assertTrue("expected to create call graph", Files.exists(cgLocation));
  }
   
  interface EdgesTest {
    void edgesTest(CallGraph staticCG, CGNode caller, MethodReference callee);
  }
 
  private static MethodReference callee(String calleeClass, String calleeMethod) {
      return MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, "L" + calleeClass), Selector.make(calleeMethod));
  }

  protected void checkEdges(CallGraph staticCG) throws IOException {
    checkEdges(staticCG, x -> true);
  }
  
  protected void checkEdges(CallGraph staticCG, Predicate<MethodReference> filter) throws IOException {
    final Set<Pair<CGNode,CGNode>> edges = HashSetFactory.make();
    check(staticCG, (staticCG1, caller, calleeRef) -> {
        Set<CGNode> nodes = staticCG1.getNodes(calleeRef);
        Assert.assertEquals("expected one node for " + calleeRef, 1, nodes.size());
        CGNode callee = nodes.iterator().next();
      
        Assert.assertTrue("no edge for " + caller + " --> " + callee, staticCG1.getPossibleSites(caller, callee).hasNext());
        Pair<CGNode,CGNode> x = Pair.make(caller, callee);
        if (edges.add(x)) {
          System.err.println("found expected edge " + caller + " --> " + callee);
        }
    }, filter);
  }
 
  protected void checkNodes(CallGraph staticCG) throws IOException {
    checkNodes(staticCG, x -> true);
  }

  protected void checkNodes(CallGraph staticCG, Predicate<MethodReference> filter) throws IOException {
    final Set<MethodReference> notFound = HashSetFactory.make();
    check(staticCG, (staticCG1, caller, callee) -> {
      boolean checkForCallee = !staticCG1.getNodes(callee).isEmpty();
      if (!checkForCallee) {
        notFound.add(callee);
      } else {
        System.err.println("found expected node " + callee);
      }
    }, filter);
    
    Assert.assertTrue("could not find " + notFound, notFound.isEmpty());
  }
 
  protected void check(CallGraph staticCG, EdgesTest test, Predicate<MethodReference> filter) throws IOException {
    int lines = 0;
    try (final BufferedReader dynamicEdgesFile = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(cgLocation))))) {
      String line;
      loop: while ((line = dynamicEdgesFile.readLine()) != null) {
        if (line.startsWith("call to") || line.startsWith("return from")) {
          continue;
        }
        
        lines++;
        StringTokenizer edge = new StringTokenizer(line, "\t");
        
        CGNode caller;
        String callerClass = edge.nextToken();
        if ("root".equals(callerClass)) {
          caller = staticCG.getFakeRootNode();
        } else if ("clinit".equals(callerClass)) {
            caller = staticCG.getFakeWorldClinitNode();
        } else if ("callbacks".equals(callerClass)) {
            continue loop;
        } else {
          String callerMethod = edge.nextToken();
          if (callerMethod.startsWith("lambda$")) {
            continue loop;
          }
          MethodReference callerRef = MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, "L" + callerClass), Selector.make(callerMethod));
          Set<CGNode> nodes = staticCG.getNodes(callerRef);
          if (! filter.test(callerRef)) {
            continue loop;
          }
          Assert.assertEquals(callerRef.toString(), 1, nodes.size());
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
    }
    
    Assert.assertTrue("more than one edge", lines > 0);
  }
  
}
