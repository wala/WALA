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
package com.ibm.wala.eclipse.cg.model;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.cast.js.client.JavaScriptAnalysisEngine;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.util.WebUtil;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.client.impl.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WalaException;

abstract public class WalaProjectCGModel implements WalaCGModel {

  protected AbstractAnalysisEngine engine;

  protected CallGraph callGraph;

  protected Collection roots;

  protected WalaProjectCGModel(IJavaProject project) {
    this.engine = new EclipseProjectSourceAnalysisEngine(project) {
      protected Entrypoints 
        makeDefaultEntrypoints(AnalysisScope scope, ClassHierarchy cha) 
      {
	return getEntrypoints(scope, cha);
      }
    };
  }

  protected WalaProjectCGModel(String htmlScriptFile) {
    this.engine = new JavaScriptAnalysisEngine() {

      {
	setTranslatorFactory(
	  new JavaScriptTranslatorFactory.CAstRhinoFactory());
      }

      protected Entrypoints 
        makeDefaultEntrypoints(AnalysisScope scope, ClassHierarchy cha) 
      {
	return getEntrypoints(scope, cha);
      }
    };

    SourceFileModule script = WebUtil.extractScriptFromHTML(htmlScriptFile);
    engine.setModuleFiles(Collections.singleton(script));
  }

  public void buildGraph() throws WalaException {
    callGraph = engine.buildDefaultCallGraph();
    roots = inferRoots(callGraph);
  }

  public CallGraph getGraph() {
    return callGraph;
  }
    
  public Collection getRoots() {
    return roots;
  }

  abstract protected Entrypoints getEntrypoints(AnalysisScope scope, ClassHierarchy cha);

  abstract protected Collection inferRoots(CallGraph cg) throws WalaException;

}
