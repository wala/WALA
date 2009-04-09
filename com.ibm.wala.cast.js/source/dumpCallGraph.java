/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.Util;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

class dumpCallGraph {

  public static void main(String[] args) throws Exception {

    JavaScriptLoaderFactory loaders = Util.makeLoaders();
    AnalysisScope scope = Util.makeScope(args, loaders, JavaScriptLoader.JS);
    IClassHierarchy cha = Util.makeHierarchy(scope, loaders);
    Iterable<Entrypoint> roots = Util.makeScriptRoots(cha);
    AnalysisOptions options = Util.makeOptions(scope, cha, roots);
    AnalysisCache cache = Util.makeCache();
    
    JSCFABuilder builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, null, ZeroXInstanceKeys.ALLOCATIONS, false);

    CallGraph cg = builder.makeCallGraph(options);

    System.err.println(cg);
  }

}
