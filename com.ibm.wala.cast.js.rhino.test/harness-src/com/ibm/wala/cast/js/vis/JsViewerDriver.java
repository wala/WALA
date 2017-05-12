/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.vis;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.DomLessSourceExtractor;
import com.ibm.wala.cast.js.html.IdentityUrlResolver;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class JsViewerDriver extends JSCallGraphBuilderUtil {
	public static void main(String args[]) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException, Error, WalaException {

		if (args.length != 1){
			System.out.println("Usage: <URL of html page to analyze>");
			System.exit(1);
		}
		boolean domless = false;
		
		URL url = new URL(args[0]); 
		
		// computing CG + PA
		JSCallGraphBuilderUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JavaScriptLoader.addBootstrapFile(WebUtil.preamble);

		SourceModule[] sources = getSources(domless, url);
		
		JSCFABuilder builder = makeCGBuilder(new WebPageLoaderFactory(translatorFactory), sources, CGBuilderType.ZERO_ONE_CFA, AstIRFactory.makeDefaultFactory());
		builder.setBaseURL(url);

		CallGraph cg = builder.makeCallGraph(builder.getOptions());
		PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();

		@SuppressWarnings("unused")
		JsViewer jsViewer = new JsViewer(cg, pa);
	}

	private static SourceModule[] getSources(boolean domless, URL url)
			throws IOException, Error {
		JSSourceExtractor sourceExtractor;
		if (domless ){
			sourceExtractor = new DomLessSourceExtractor(); 
		} else {
			sourceExtractor = new DefaultSourceExtractor();
		}

		Set<MappedSourceModule> sourcesMap = sourceExtractor.extractSources(url, new JerichoHtmlParser(), new IdentityUrlResolver());
		SourceModule[] sources = new SourceFileModule[sourcesMap.size()];
		int i = 0;
		for (SourceModule m : sourcesMap){
			sources[i++] = m;
		}
		return sources;
	}

}
