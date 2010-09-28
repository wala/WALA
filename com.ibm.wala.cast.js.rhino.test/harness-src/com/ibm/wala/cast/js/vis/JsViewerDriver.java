package com.ibm.wala.cast.js.vis;

import java.io.IOException;
import java.net.URL;

import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.test.Util;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
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
		
		URL url = new URL(args[0]);
		
		// computing CG + PA
		Util.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JSCFABuilder builder = Util.makeHTMLCGBuilder(url);
		CallGraph cg = builder.makeCallGraph(builder.getOptions());
		
		PointerAnalysis pa = builder.getPointerAnalysis();

		new JsViewer(cg, pa);
	}

}
