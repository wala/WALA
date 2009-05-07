/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.java.client.polyglot;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class EclipseProjectAnalysisEngine extends AbstractAnalysisEngine {

  protected final IPath workspaceRootPath;

  protected final IJavaProject project;

  protected final EclipseProjectPath ePath;

  public EclipseProjectAnalysisEngine(IJavaProject project) throws IOException, CoreException {
    super();
    this.project = project;
    this.workspaceRootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    assert project != null;
    assert workspaceRootPath != null;
    this.ePath = createProjectPath(project);
    // setCallGraphBuilderFactory(new ZeroCFABuilderFactory());
  }

  protected EclipseProjectPath createProjectPath(IJavaProject project) throws IOException, CoreException {
    return EclipseProjectPath.make(project);
  }

  @Override
  public void buildAnalysisScope() throws IOException {
    super.scope = ePath.toAnalysisScope(new File(getExclusionsFile()));
  }

  public EclipseProjectPath getEclipseProjectPath() {
    return ePath;
  }

  @Override
  protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    return new ZeroCFABuilderFactory().make(options, cache, cha, scope, false);
  }
}
