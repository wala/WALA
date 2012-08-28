package com.ibm.wala.ide.test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.tests.util.EclipseTestUtil;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ide.util.JavaScriptHeadlessUtil;
import com.ibm.wala.ide.util.JsdtUtil;
import com.ibm.wala.ide.util.JsdtUtil.CGInfo;
import com.ibm.wala.ipa.callgraph.AnalysisScope;

public class JSProjectScopeTest {

  private static final String jsTestDataProject = "com.ibm.wala.cast.js.test.data";

  @BeforeClass
  public static void beforeClass() {
    EclipseTestUtil.importZippedProject(Activator.getDefault(), jsTestDataProject, "test_js_project.zip", new NullProgressMonitor());
    System.err.println("finish importing project");
  }

  @AfterClass
  public static void afterClass() {
    EclipseTestUtil.destroyProject(jsTestDataProject);
  }

  @Test
  public void testOpenProject() {
    IJavaScriptProject p = JavaScriptHeadlessUtil.getJavaScriptProjectFromWorkspace(jsTestDataProject);
    System.err.println(p);
    Assert.assertTrue("cannot find project", p != null);
  }

  @Test
  public void testProjectScope() throws IOException, CoreException {
    IJavaScriptProject p = JavaScriptHeadlessUtil.getJavaScriptProjectFromWorkspace(jsTestDataProject);
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    AnalysisScope s = JavaScriptEclipseProjectPath.make(p).toAnalysisScope(new CAstAnalysisScope(JSCallGraphUtil.makeLoaders(), Collections.singleton(JavaScriptLoader.JS)));
    System.err.println(s);
    Assert.assertTrue("cannot make scope", s != null);
    Assert.assertFalse("cannot find files", s.getModules(JavaScriptTypes.jsLoader).isEmpty());
  }

  @Test
  public void testParsing() throws IOException, CoreException {
    Set<ModuleEntry> mes = JsdtUtil.getJavaScriptCodeFromProject(jsTestDataProject);
    CGInfo info = JsdtUtil.buildJSDTCallGraph(mes);
    
    System.err.println(info.calls.size());
    System.err.println("call graph:\n" + info.cg);
    Assert.assertTrue("cannot find any function calls", info.calls.size()>0);
    Assert.assertTrue("cannot find any cg nodes", info.cg.getNumberOfNodes()>0);
  }

}
