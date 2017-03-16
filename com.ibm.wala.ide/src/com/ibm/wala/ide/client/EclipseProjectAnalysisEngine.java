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
package com.ibm.wala.ide.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;

abstract public class EclipseProjectAnalysisEngine<P, I extends InstanceKey> extends AbstractAnalysisEngine<I> {

  protected final P project;
  
  protected final IPath workspaceRootPath;

  protected EclipseProjectPath<?,P> ePath;

  public EclipseProjectAnalysisEngine(P project) throws IOException, CoreException {
    super();
    this.project = project;
    this.workspaceRootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    assert project != null;
    assert workspaceRootPath != null;
  }

  abstract protected EclipseProjectPath<?,P> createProjectPath(P project) throws IOException, CoreException;

  @Override
  abstract protected CallGraphBuilder<I> getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache);

  abstract protected AnalysisScope makeAnalysisScope();
  
  @Override
  public void buildAnalysisScope() throws IOException {
    try {
      ePath = createProjectPath(project);
      super.scope = ePath.toAnalysisScope(makeAnalysisScope());
      if (getExclusionsFile() != null) {
        try (final InputStream is = new File(getExclusionsFile()).exists()? new FileInputStream(getExclusionsFile()): FileProvider.class.getClassLoader().getResourceAsStream(getExclusionsFile())) {
          scope.setExclusions(new FileOfClasses(is));
        }
      }
    } catch (CoreException e) {
      assert false : e.getMessage();
    }
  }

  public EclipseProjectPath<?,P> getEclipseProjectPath() {
    return ePath;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    if (super.getClassHierarchy() == null) {
      setClassHierarchy( buildClassHierarchy() );
    }

    return super.getClassHierarchy();
  }

}
