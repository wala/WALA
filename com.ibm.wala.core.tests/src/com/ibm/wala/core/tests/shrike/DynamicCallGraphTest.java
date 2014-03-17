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
import org.junit.Test;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrike.cg.DynamicCallGraph;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.io.TemporaryFile;

public class DynamicCallGraphTest extends WalaTestCase {
  
  private static URL testJarLocation;
  static {
    testJarLocation = DynamicCallGraphTest.class.getClassLoader().getResource("com.ibm.wala.core.testdata_1.0.0.jar");
    if (testJarLocation == null) {
      testJarLocation = DynamicCallGraphTest.class.getClassLoader().getResource("com.ibm.wala.core.testdata-1.3.4-SNAPSHOT.jar");      
    }
    assert testJarLocation != null;
  }
  
  private boolean instrumentedJarBuilt = false;
  
  private void instrument() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException {
    if (! instrumentedJarBuilt) {
      DynamicCallGraph.main(new String[]{testJarLocation.getPath(), "-o", "/tmp/test.jar"});
      Assert.assertTrue("expected to create /tmp/test.jar", new File("/tmp/test.jar").exists());   
      instrumentedJarBuilt = true;
    }
  }
  
  private void run(String exclusionsFile) throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    String shrikeBin = new File("../com.ibm.wala.shrike/bin/").getCanonicalPath();
    String utilBin = new File("../com.ibm.wala.util/bin/").getCanonicalPath();
    URLClassLoader jcl = new URLClassLoader(new URL[]{ new URL("file:///tmp/test.jar"), new URL("file://" + shrikeBin + "/"), new URL("file://" + utilBin + "/") }, DynamicCallGraphTest.class.getClassLoader().getParent());
 
    Class<?> testClass = jcl.loadClass("dynamicCG.MainClass");
    Assert.assertNotNull(testClass);
    Method testMain = testClass.getDeclaredMethod("main", String[].class);
    Assert.assertNotNull(testMain);

    System.setProperty("dynamicCGFile", "/tmp/cg.txt");
    if (exclusionsFile != null) {
      File tmpFile = TemporaryFile.urlToFile("exclusions.txt", getClass().getClassLoader().getResource(exclusionsFile));
      System.setProperty("dynamicCGFilter", tmpFile.getCanonicalPath());
    }
    try {
      testMain.invoke(null, (Object)new String[0]);      
    } catch (Throwable e) {
      // exceptions allowed here, just collecting CG
    }
    
    // the VM is not exiting, so stop tracing explicitly
    Class<?> runtimeClass = jcl.loadClass("com.ibm.wala.shrike.cg.Runtime");
    Assert.assertNotNull(runtimeClass);
    Method endTrace = runtimeClass.getDeclaredMethod("endTrace");
    Assert.assertNotNull(endTrace);
    endTrace.invoke(null);

    Assert.assertTrue("expected to create call graph", new File(System.getProperty("dynamicCGFile")).exists());
  }
  
  private CallGraph staticCG(String exclusionsFile) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(TestConstants.WALA_TESTDATA, exclusionsFile != null? exclusionsFile: CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchy.make(scope);
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, "LdynamicCG/MainClass");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    return CallGraphTestUtil.buildZeroOneCFA(options, new AnalysisCache(), cha, scope, false);
  }
  
  private void check(CallGraph staticCG) throws IOException {
    BufferedReader dynamicEdgesFile = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(System.getProperty("dynamicCGFile")))));
    String line;
    while ((line = dynamicEdgesFile.readLine()) != null) {
      StringTokenizer edge = new StringTokenizer(line, "\t");
      
      CGNode caller;
      String callerClass = edge.nextToken();
      if ("root".equals(callerClass)) {
        caller = staticCG.getFakeRootNode();
      } else {
        String callerMethod = edge.nextToken();
        Set<CGNode> nodes = staticCG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, "L" + callerClass), Selector.make(callerMethod)));
        Assert.assertEquals(1, nodes.size());
        caller = nodes.iterator().next();
      }
      
      String calleeClass = edge.nextToken();
      String calleeMethod = edge.nextToken();
      Set<CGNode> nodes = staticCG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, "L" + calleeClass), Selector.make(calleeMethod)));
      Assert.assertEquals(1, nodes.size());
      CGNode callee = nodes.iterator().next();
      
      Assert.assertTrue("no edge for " + caller + " --> " + callee, staticCG.getPossibleSites(caller, callee).hasNext());
      System.err.println("found expected edge" + caller + " --> " + callee);
    }
    
    dynamicEdgesFile.close();
  }
  
  @Test
  public void testGraph() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassHierarchyException, CancelException  {
    instrument();
    run(null);
    CallGraph staticCG = staticCG(null);
    check(staticCG);
  }

  @Test
  public void testExclusions() throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassHierarchyException, CancelException  {
    instrument();
    run("ShrikeTestExclusions.txt");
    CallGraph staticCG = staticCG("ShrikeTestExclusions.txt");
    check(staticCG);
  }
}
