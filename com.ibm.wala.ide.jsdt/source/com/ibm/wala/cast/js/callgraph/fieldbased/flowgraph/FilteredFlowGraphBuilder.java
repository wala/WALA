package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph;

import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.functions.Function;

public class FilteredFlowGraphBuilder extends FlowGraphBuilder {

	private final Function<IMethod, Boolean> filter;
	
	public FilteredFlowGraphBuilder(IClassHierarchy cha, AnalysisCache cache, JavaScriptConstructorFunctions selector, Function<IMethod, Boolean> filter) {
		super(cha, cache, selector);
		this.filter = filter;
	}

	@Override
	public void visitFunction(FlowGraph flowgraph, IMethod method) {
		if (filter.apply(method)) {
			super.visitFunction(flowgraph, method);
		}
	}

	
}
