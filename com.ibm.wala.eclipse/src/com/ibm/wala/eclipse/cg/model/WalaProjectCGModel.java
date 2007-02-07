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

import com.ibm.wala.cast.java.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.cast.js.client.JavaScriptAnalysisEngine;
import com.ibm.wala.cast.js.translator.*;
import com.ibm.wala.cast.js.util.*;
import com.ibm.wala.client.impl.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.warnings.WalaException;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jdt.core.*;

import java.util.*;

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
