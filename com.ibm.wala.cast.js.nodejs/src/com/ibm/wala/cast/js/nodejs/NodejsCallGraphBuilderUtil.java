/******************************************************************************
 * Copyright (c) 2002 - 2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brian Pfretzschner - initial implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.nodejs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptEntryPoints;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.WalaException;

/**
 * @author Brian Pfretzschner &lt;brian.pfretzschner@gmail.com&gt;
 */
public class NodejsCallGraphBuilderUtil extends JSCallGraphUtil {

	public static PropagationCallGraphBuilder makeCGBuilder(File mainFile)
			throws IOException, IllegalArgumentException, WalaException {
		return makeCGBuilder(mainFile.getParentFile(), mainFile);
	}

	public static PropagationCallGraphBuilder makeCGBuilder(File workingDir, File mainFile)
			throws IOException, IllegalArgumentException, WalaException {
		JavaScriptTranslatorFactory translatorFactory = new CAstRhinoTranslatorFactory();
		JSCallGraphUtil.setTranslatorFactory(translatorFactory);

		Language language = JavaScriptLoader.JS;
		Collection<Language> languages = Collections.singleton(language);

		IRFactory<IMethod> irFactory = new AstIRFactory.AstDefaultIRFactory<>();
		IAnalysisCacheView cache = new AnalysisCacheImpl(irFactory);

		JavaScriptLoaderFactory loaders = new JavaScriptLoaderFactory(translatorFactory, null);

		SourceFileModule mainSourceModule = CAstCallGraphUtil.makeSourceModule(mainFile.toURI().toURL(),
				mainFile.getName());
		String mainFileClassName = NodejsRequiredSourceModule.convertFileToClassName(workingDir, mainFile);

		Module[] files = new Module[] {
				JSCallGraphUtil.getPrologueFile("prologue.js"),
				JSCallGraphUtil.getPrologueFile("extended-prologue.js"),
				new NodejsRequiredSourceModule(mainFileClassName, mainFile, mainSourceModule) };

		CAstAnalysisScope scope = new CAstAnalysisScope(files, loaders, languages);

		IClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, language, null);
		com.ibm.wala.cast.util.Util.checkForFrontEndErrors(cha);

		// Make Script Roots
		Iterable<Entrypoint> roots = new JavaScriptEntryPoints(cha, loaders.getTheLoader());

		// Make Options
		JSAnalysisOptions options = new JSAnalysisOptions(scope, roots);
		options.setUseConstantSpecificKeys(true);
		options.setUseStacksForLexicalScoping(true);
		options.setHandleCallApply(true);
		// Important to be able to identify what file are required
		options.setTraceStringConstants(true);

		com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);

		MethodTargetSelector baseSelector = new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector());
		NodejsRequireTargetSelector requireTargetSelector = new NodejsRequireTargetSelector(workingDir, baseSelector);
		options.setSelector(requireTargetSelector);

		JSCFABuilder builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null,
				ZeroXInstanceKeys.ALLOCATIONS, true);

		// A little hacky, but the instance of RequireTargetSelector is required to build the CallGraphBuilder
		// and the RequireTargetSelector also needs the CallGraphBuilder instance.
		requireTargetSelector.setCallGraphBuilder(builder);

		ContextSelector contextSelector = new PropertyNameContextSelector(cache, 2, builder.getContextSelector());
		builder.setContextSelector(contextSelector);

		return builder;
	}
}
