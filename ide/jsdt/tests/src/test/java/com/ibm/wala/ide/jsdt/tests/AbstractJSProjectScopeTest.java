/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ide.jsdt.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.js.client.EclipseJavaScriptAnalysisEngine;
import com.ibm.wala.cast.js.client.EclipseJavaScriptAnalysisEngine.BuilderType;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;
import com.ibm.wala.ide.util.JavaScriptHeadlessUtil;
import com.ibm.wala.ide.util.JsdtUtil;
import com.ibm.wala.ide.util.JsdtUtil.CGInfo;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public abstract class AbstractJSProjectScopeTest {

  protected final ZippedProjectData project;

  public AbstractJSProjectScopeTest(ZippedProjectData project) {
    this.project = project;
  }

  @Test
  public void testOpenProject() {
    IJavaScriptProject p =
        JavaScriptHeadlessUtil.getJavaScriptProjectFromWorkspace(project.projectName);
    System.err.println(p);
    assertNotNull(p, "cannot find project");
  }

  @Test
  public void testProjectScope() throws IOException, CoreException {
    IJavaScriptProject p =
        JavaScriptHeadlessUtil.getJavaScriptProjectFromWorkspace(project.projectName);
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    AnalysisScope s =
        makeProjectPath(p)
            .toAnalysisScope(
                new CAstAnalysisScope(
                    JSCallGraphUtil.makeLoaders(), Collections.singleton(JavaScriptLoader.JS)));
    System.err.println(s);
    assertNotNull(s, "cannot make scope");
    assertFalse(s.getModules(JavaScriptTypes.jsLoader).isEmpty(), "cannot find files");
  }

  protected JavaScriptEclipseProjectPath makeProjectPath(IJavaScriptProject p)
      throws IOException, CoreException {
    return JavaScriptEclipseProjectPath.make(p, Collections.<Pair<String, Plugin>>emptySet());
  }

  @Disabled("works for me on Eclipse Luna, but I cannot make it work with maven")
  @Test
  public void testParsing() throws IOException, CoreException {
    Set<ModuleEntry> mes = JsdtUtil.getJavaScriptCodeFromProject(project.projectName);
    CGInfo info = JsdtUtil.buildJSDTCallGraph(mes);

    System.err.println(info.calls.size());
    System.err.println("call graph:\n" + info.cg);
    assertFalse(info.calls.isEmpty(), "cannot find any function calls");
    assertTrue(info.cg.getNumberOfNodes() > 0, "cannot find any cg nodes");
  }

  @Test
  public void testEngine() throws IOException, IllegalArgumentException {
    IJavaScriptProject p =
        JavaScriptHeadlessUtil.getJavaScriptProjectFromWorkspace(project.projectName);
    EclipseJavaScriptAnalysisEngine e = makeAnalysisEngine(p);
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    e.buildAnalysisScope();
    IClassHierarchy cha = e.getClassHierarchy();
    // System.err.println(cha);
    assertNotNull(cha);
  }

  protected EclipseJavaScriptAnalysisEngine makeAnalysisEngine(IJavaScriptProject p) {
    return new EclipseJavaScriptAnalysisEngine(p, BuilderType.REFLECTIVE);
  }
}
