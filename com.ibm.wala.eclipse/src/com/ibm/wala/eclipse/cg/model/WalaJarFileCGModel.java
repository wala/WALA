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


abstract public class WalaJarFileCGModel implements WalaCGModel {

       /**
	 * Specifies the path of the jars files to be analyzed, each jar file separated by ';'
	 */
	protected String appJar;

	protected CallGraph callGraph;

	protected Collection roots;

	/**
	 * @param appJar Specifies the path of the jars files to be analyzed, each jar file separated by ';'
	 */	
	public WalaJarFileCGModel(String appJar) {
		this.appJar = appJar;
	}

	/**
	 * @see CallGraphBuilderImpl.processImpl
	 * warning: this is bypassing emf and may cause problems
	 */
	public void buildGraph() throws WalaException {
		EMFScopeWrapper escope = createAnalysisScope();  
		callGraph = createCallGraph(escope);	    
		roots = inferRoots(callGraph);
	}

	public CallGraph getGraph() {
		return callGraph;
	}

	public Collection getRoots() {
		return roots;
	}

	/**
	 * @see SWTCallGraph
	 */
	protected EMFScopeWrapper createAnalysisScope() throws WalaException {
		EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

		escope.setExclusionFileName("J2SEClassHierarchyExclusions.xml");

		// generate a WALA-consumable wrapper around the incoming scope object
		EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
		return scope;
	}

	abstract protected CallGraph createCallGraph(EMFScopeWrapper escope) throws WalaException;

	abstract protected Collection inferRoots(CallGraph cg) throws WalaException;

	public ApplicationWindow makeUI(Graph graph, Collection<?> roots) throws WalaException {
		final SWTTreeViewer v = new SWTTreeViewer();
		v.setGraphInput(graph);
		v.setRootsInput(roots);
		v.run();
		return v.getApplicationWindow();
	}
}
