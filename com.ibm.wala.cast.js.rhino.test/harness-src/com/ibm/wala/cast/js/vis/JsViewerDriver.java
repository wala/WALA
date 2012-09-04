package com.ibm.wala.cast.js.vis;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
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
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class JsViewerDriver extends JSCallGraphBuilderUtil {
	public static void main(String args[]) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {

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
		PointerAnalysis pa = builder.getPointerAnalysis();

		new JsViewer(cg, pa);
	}

	private static SourceModule[] getSources(boolean domless, URL url)
			throws IOException {
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
