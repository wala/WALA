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

import java.io.*;
import java.net.*;
import java.util.*;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.loader.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

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

    Assertions._assert(hackedName.endsWith(scriptName), scriptName + " does not match file " + script.getFile());

    return new SourceFileModule(scriptFile, scriptName);
  }

    public static AnalysisScope makeScope(String[] files, SingleClassLoaderFactory loaders, Language language) throws IOException {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders);
    result.addLanguageToScope(language);
    return result;
  }

  public static AnalysisScope makeScope(SourceFileModule[] files, SingleClassLoaderFactory loaders, Language language) throws IOException {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders);
    result.addLanguageToScope(language);
    return result;
  }

  public static AnalysisScope makeScope(URL[] files, SingleClassLoaderFactory loaders, Language language) throws IOException {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders);
    result.addLanguageToScope(language);
    return result;
  }

  public static AnalysisCache makeCache(boolean keepIRs) {
    return new AnalysisCache(AstIRFactory.makeDefaultFactory(keepIRs));
  }

  public static void dumpCG(PropagationCallGraphBuilder builder, CallGraph CG) {
    Trace.println(CG);

    for (Iterator x = CG.iterator(); x.hasNext();) {
      CGNode N = (CGNode) x.next();
      Trace.println("\nIR of node " + N);
      IR ir = N.getIR();
      if (ir != null) {
        Trace.println(ir);
      } else {
        Trace.println("no IR!");
      }
    }

    PointerAnalysis PA = builder.getPointerAnalysis();
    for (Iterator x = PA.getPointerKeys().iterator(); x.hasNext();) {
      PointerKey n = (PointerKey) x.next();
      try {
        Trace.println(n + " --> " + PA.getPointsToSet(n));
      } catch (Throwable e) {
        Trace.println("error computing set for " + n);
      }
    }
  }

  public static SourceFileModule[] handleFileNames(String[] fileNameArgs) {
    SourceFileModule[] fileNames = new SourceFileModule[fileNameArgs.length];
    for (int i = 0; i < fileNameArgs.length; i++) {
      if (new File(fileNameArgs[i]).exists()) {
        try {
          fileNames[i] = Util.makeSourceModule(new URL("file:" + fileNameArgs[i]), fileNameArgs[i]);
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
