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
package com.ibm.wala.cast.ipa.callgraph;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.debug.Assertions;

public class Util {

  public static SourceFileModule makeSourceModule(URL script, String dir, String name) {
    // DO NOT use File.separator here, since this name is matched against
    // URLs. It seems that, in DOS, URL.getFile() does not return a
    // \-separated file name, but rather one with /'s. Rather makes one
    // wonder why the function is called get_File_ :(
    return makeSourceModule(script, dir + "/" + name);
  }

  public static SourceFileModule makeSourceModule(URL script, String scriptName) {
    String hackedName = script.getFile().replaceAll("%5c", "/").replaceAll("%20", " ");

    File scriptFile = new File(hackedName);

    assert hackedName.endsWith(scriptName) : scriptName + " does not match file " + script.getFile();

    return new SourceFileModule(scriptFile, scriptName);
  }

  public static AnalysisScope makeScope(String[] files, SingleClassLoaderFactory loaders, Language language) throws IOException {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders, Collections.singleton(language));
    return result;
  }

  public static AnalysisScope makeScope(SourceModule[] files, SingleClassLoaderFactory loaders, Language language)
      throws IOException {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders, Collections.singleton(language));
    return result;
  }

  public static AnalysisScope makeScope(URL[] files, SingleClassLoaderFactory loaders, Language language) throws IOException {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders, Collections.singleton(language));
    return result;
  }

  public static AnalysisCache makeCache() {
    return new AnalysisCache(AstIRFactory.makeDefaultFactory());
  }

  public static void dumpCG(PointerAnalysis PA, CallGraph CG) {
    for (Iterator x = CG.iterator(); x.hasNext();) {
      CGNode N = (CGNode) x.next();
      System.err.println("\ncallees of node " + N.getMethod() + " " + N.getGraphNodeId());
      for(Iterator<? extends CGNode> ns = CG.getSuccNodes(N); ns.hasNext(); ) {
        System.err.println("\n  " + ns.next().getGraphNodeId());
      }
      System.err.println("\nIR of node " + N.getGraphNodeId());
      IR ir = N.getIR();
      if (ir != null) {
        System.err.println(ir);
      } else {
        System.err.println("no IR!");
      }
    }

    System.err.println("pointer analysis");
    for (Iterator x = PA.getPointerKeys().iterator(); x.hasNext();) {
      PointerKey n = (PointerKey) x.next();
      try {
        System.err.println((n + " --> " + PA.getPointsToSet(n)));
      } catch (Throwable e) {
        System.err.println(("error computing set for " + n));
      }
    }
  }

  public static SourceFileModule[] handleFileNames(String[] fileNameArgs) {
    SourceFileModule[] fileNames = new SourceFileModule[fileNameArgs.length];
    for (int i = 0; i < fileNameArgs.length; i++) {
      if (new File(fileNameArgs[i]).exists()) {
        try {
          fileNames[i] = Util.makeSourceModule(new File(fileNameArgs[i]).toURI().toURL(), fileNameArgs[i]);
        } catch (MalformedURLException e) {
          Assertions.UNREACHABLE(e.toString());
        }
      } else {
        URL url = Util.class.getClassLoader().getResource(fileNameArgs[i]);
        fileNames[i] = Util.makeSourceModule(url, fileNameArgs[i]);
      }
    }

    return fileNames;
  }

}
