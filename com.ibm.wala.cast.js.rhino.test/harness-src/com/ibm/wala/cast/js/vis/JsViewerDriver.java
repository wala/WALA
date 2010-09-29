package com.ibm.wala.cast.js.vis;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.DomLessSourceExtractor;
import com.ibm.wala.cast.js.html.FileMapping;
import com.ibm.wala.cast.js.html.IdentityUrlResover;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import com.ibm.wala.cast.js.html.jericho.JerichoHtmlParser;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.test.Util;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.Generator;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class JsViewerDriver {
	public static void main(String args[]) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {

		if (args.length != 1){
			System.out.println("Usage: <URL of html page to analyze>");
			System.exit(1);
		}
		boolean domless = false;
		
		URL url = new URL(args[0]); 
		
		// computing CG + PA
		Util.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JavaScriptLoader.addBootstrapFile(Generator.preamble);

		SourceFileModule[] sources = getSources(domless, url);
		
		JSCFABuilder builder = Util.makeCGBuilder(sources, false);
    builder.setBaseURL(url);

		CallGraph cg = builder.makeCallGraph(builder.getOptions());
		PointerAnalysis pa = builder.getPointerAnalysis();

		new JsViewer(cg, pa);
	}

	private static SourceFileModule[] getSources(boolean domless, URL url)
			throws IOException {
		JSSourceExtractor sourceExtractor;
		if (domless ){
			sourceExtractor = new DomLessSourceExtractor(); 
		} else {
			sourceExtractor = new DefaultSourceExtractor();
		}

		Map<SourceFileModule, FileMapping> sourcesMap = sourceExtractor.extractSources(url, new JerichoHtmlParser(), new IdentityUrlResover());
		SourceFileModule[] sources = new SourceFileModule[sourcesMap.size()];
		int i = 0;
		for (SourceFileModule m : sourcesMap.keySet()){
			sources[i++] = m;
		}
		return sources;
	}

}
