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

import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.graph.InferGraphRootsImpl;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * @author aying
 */
public class WalaCGModelWithMain extends WalaCGModel {

	/*
	 * @see WalaCGModel
	 */
	public WalaCGModelWithMain(String appJar) {
		super(appJar);
	}

	/**
	 * @see SWTCallGraph
	 */
	@Override
	protected CallGraph createCallGraph(EMFScopeWrapper scope) throws WalaException {

		// TODO: return the warning set (need a CAPA type)
		// invoke DOMO to build a DOMO class hierarchy object
		WarningSet warnings = new WarningSet();
		ClassHierarchy cha = ClassHierarchy.make(scope, warnings);

		Entrypoints entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

		// //
		// build the call graph
		// //
		com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, cha, scope, warnings, null, null);
		CallGraph cg = builder.makeCallGraph(options);
		return cg;
	}

	/**
	 * @see SWTCallGraph
	 */
	@Override
	protected Collection inferRoots(CallGraph cg) throws WalaException {
		return InferGraphRootsImpl.inferRoots(cg);
	}
}
