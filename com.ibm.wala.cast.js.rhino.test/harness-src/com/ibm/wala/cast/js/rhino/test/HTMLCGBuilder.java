package com.ibm.wala.cast.js.rhino.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import junit.framework.Assert;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.wala.cast.js.ipa.callgraph.ForInContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionDotCallTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.CorrelatedPairExtractorFactory;
import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.ProgressMaster;
import com.ibm.wala.util.ProgressMonitorDelegate;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

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
		public PointerAnalysis pa;
		
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
	 * @param automated_extraction
	 * 			whether to automatically extract correlated pairs
	 * @throws IOException 
	 * @throws ClassHierarchyException 
	 */
	public static CGBuilderResult buildHTMLCG(String src, int timeout, boolean automated_extraction) 
			throws ClassHierarchyException, IOException {
		CGBuilderResult res = new CGBuilderResult();
		URL url = null;
		try {
			url = toUrl(src);
		} catch (MalformedURLException e1) {
			Assert.fail("Could not find page to analyse: " + src);
		}
		com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		if(automated_extraction)
			com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setPreprocessor(new CorrelatedPairExtractorFactory(new CAstRhinoTranslatorFactory(), url));
		JSCFABuilder builder = null;
		try {
			builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url);
			builder.setContextSelector(new ForInContextSelector(2, builder.getContextSelector()));
			builder.setContextSelector(new ForInContextSelector(3, builder.getContextSelector()));
			ProgressMaster master = ProgressMaster.make(new NullProgressMonitor());
			if (timeout > 0) {
				master.setMillisPerWorkItem(timeout * 1000);
				master.beginTask("runSolver", 1);
			}
			long start = System.currentTimeMillis();
			CallGraph cg = timeout > 0 ? builder.makeCallGraph(builder.getOptions(),
					ProgressMonitorDelegate.createProgressMonitorDelegate(master)) : builder.makeCallGraph(builder.getOptions());
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
			File f = FileProvider.getFileFromClassLoader(src, HTMLCGBuilder.class.getClassLoader());
			URL url = f.toURI().toURL();
			return url;
		} catch (FileNotFoundException fnfe) {
			return new URL(src);
		}
	}

	/**
	 * Usage: HTMLCGBuilder -src path_to_html_file -timeout timeout_in_seconds
	 * timeout argument is optional and defaults to {@link #DEFAULT_TIMEOUT}.
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
		int timeout;
		if (parsedArgs.containsKey("timeout")) {
			timeout = Integer.parseInt(parsedArgs.getProperty("timeout"));
		} else {
			timeout = DEFAULT_TIMEOUT;
		}
		// suppress debug output
		JavaScriptFunctionDotCallTargetSelector.WARN_ABOUT_IMPRECISE_CALLGRAPH = false;
		CGBuilderResult res = buildHTMLCG(src, timeout, true);
		if(res.construction_time == -1)
			System.out.println("TIMED OUT");
		else
			System.out.println("Call graph construction took " + res.construction_time/1000.0 + " seconds");
	}
}
