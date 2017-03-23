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
package com.ibm.wala.cast.js.rhino.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.junit.Assert;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionDotCallTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.RecursionCheckContextSelector;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.ProgressMaster;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileUtil;

/**
 * Utility class for building call graphs of HTML pages.
 * 
 * @author mschaefer
 *
 */
public class HTMLCGBuilder {
	public static final int DEFAULT_TIMEOUT = 120;
	
	/**
	 * Simple struct-like type to hold results of call graph construction.
	 * 
	 * @author mschaefer
	 *
	 */
	public static class CGBuilderResult {
		/** time it took to build the call graph; {@code -1} if timeout occurred */
		public long construction_time;
		
		/** builder responsible for building the call graph*/
		public JSCFABuilder builder;
		
		/** pointer analysis results; partial if {@link #construction_time} is {@code -1} */
		public PointerAnalysis<InstanceKey> pa;
		
		/** call graph; partial if {@link #construction_time} is {@code -1} */
		public CallGraph cg;
	}

	/**
	 * Build a call graph for an HTML page, optionally with a timeout.
	 * 
	 * @param src
	 *          the HTML page to analyse, can either be a path to a local file or a URL
	 * @param timeout
	 *          analysis timeout in seconds, -1 means no timeout
	 * @param fExtractor 
	 * @throws IOException 
	 * @throws ClassHierarchyException 
	 */
	public static CGBuilderResult buildHTMLCG(String src, int timeout, CGBuilderType builderType, Function<Void, JSSourceExtractor> fExtractor) 
			throws ClassHierarchyException, IOException {
		CGBuilderResult res = new CGBuilderResult();
		URL url = null;
		try {
			url = toUrl(src);
		} catch (MalformedURLException e1) {
			Assert.fail("Could not find page to analyse: " + src);
		}
		com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JSCFABuilder builder = null;
		try {
			builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url, builderType, fExtractor);
			// TODO we need to find a better way to do this ContextSelector delegation;
			// the code below belongs somewhere else!!!
			// the bound of 4 is what is needed to pass our current framework tests
			builder.setContextSelector(new RecursionCheckContextSelector(builder.getContextSelector()));
			ProgressMaster master = ProgressMaster.make(new NullProgressMonitor(), timeout * 1000, false);
			if (timeout > 0) {
				master.beginTask("runSolver", 1);
			}
			long start = System.currentTimeMillis();
			CallGraph cg = timeout > 0 ? builder.makeCallGraph(builder.getOptions(),
					master) : builder.makeCallGraph(builder.getOptions());
			long end = System.currentTimeMillis();
			master.done();
			res.construction_time = (end - start);
			res.cg = cg;
			res.pa = builder.getPointerAnalysis();
			res.builder = builder;
			return res;
		} catch (CallGraphBuilderCancelException e) {
			res.construction_time = -1;
			res.cg = e.getPartialCallGraph();
			res.pa = e.getPartialPointerAnalysis();
			res.builder = builder;
			return res;
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private static URL toUrl(String src) throws MalformedURLException {
		// first try interpreting as local file name, if that doesn't work just
		// assume it's a URL
		try {
			File f = (new FileProvider()).getFileFromClassLoader(src, HTMLCGBuilder.class.getClassLoader());
			URL url = f.toURI().toURL();
			return url;
		} catch (FileNotFoundException fnfe) {
			return new URL(src);
		}
	}

	/**
	 * Usage: HTMLCGBuilder -src path_to_html_file -timeout timeout_in_seconds -reachable function_name
	 * timeout argument is optional and defaults to {@link #DEFAULT_TIMEOUT}.
	 * reachable argument is optional.  if provided, and some reachable function name contains function_name,
	 * will print "REACHABLE"
	 * @throws IOException 
	 * @throws ClassHierarchyException 
	 * 
	 */
	public static void main(String[] args) throws ClassHierarchyException, IOException {
		Properties parsedArgs = CommandLine.parse(args);
		
		String src = parsedArgs.getProperty("src");
		if (src == null) {
			throw new IllegalArgumentException("-src argument is required");
		}

		// if src is a JS file, build trivial wrapper HTML file
		if (src.endsWith(".js")) {
			File tmpFile = File.createTempFile("HTMLCGBuilder", ".html");
			tmpFile.deleteOnExit();
			FileUtil.writeFile(tmpFile, 
					"<html>" +
					"  <head>" +
					"    <title></title>" +
					"    <script src=\"" + src + "\" type='text/javascript'></script>" +
					"  </head>" +
					"<body>" +
					"</body>" +
					"</html>");
			src = tmpFile.getAbsolutePath();
		}
		
		int timeout;
		if (parsedArgs.containsKey("timeout")) {
			timeout = Integer.parseInt(parsedArgs.getProperty("timeout"));
		} else {
			timeout = DEFAULT_TIMEOUT;
		}
		
		String reachableName = null;
		if (parsedArgs.containsKey("reachable")) {
			reachableName = parsedArgs.getProperty("reachable");
		}
		
		// suppress debug output
		JavaScriptFunctionDotCallTargetSelector.WARN_ABOUT_IMPRECISE_CALLGRAPH = false;
		
		// build call graph
		CGBuilderResult res = buildHTMLCG(src, timeout, CGBuilderType.ONE_CFA, DefaultSourceExtractor.factory);
		
		if(res.construction_time == -1)
			System.out.println("TIMED OUT");
		else
			System.out.println("Call graph construction took " + res.construction_time/1000.0 + " seconds");
		
		if (reachableName != null) {
			for (CGNode node : res.cg) {
				if (node.getMethod().getDeclaringClass().getName().toString().contains(reachableName)) {
					System.out.println("REACHABLE");
					break;
				}
			}
		}
	}
}
