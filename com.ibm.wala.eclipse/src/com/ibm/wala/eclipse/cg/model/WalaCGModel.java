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

import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.viz.SWTTreeViewer;


public interface WalaCGModel {

  void buildGraph() throws WalaException;

  CallGraph getGraph();

  Collection getRoots();

}
