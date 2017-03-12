/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.functions.Function;

public class FilteredFlowGraphBuilder extends FlowGraphBuilder {

	private final Function<IMethod, Boolean> filter;
	
	public FilteredFlowGraphBuilder(IClassHierarchy cha, IAnalysisCacheView cache, boolean fullPointerAnalysis, Function<IMethod, Boolean> filter) {
		super(cha, cache, fullPointerAnalysis);
		this.filter = filter;
	}

	@Override
	public void visitFunction(FlowGraph flowgraph, IMethod method) {
		if (filter.apply(method)) {
			super.visitFunction(flowgraph, method);
		}
	}

	
}
