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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;

public class CAstCallGraphUtil {

  /**
   * flag to prevent dumping of verbose call graph / pointer analysis output
   */
  public static boolean AVOID_DUMP = true;

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

    return new SourceFileModule(scriptFile, scriptName, null) {
      @Override
      public InputStream getInputStream() {
        BOMInputStream bs = new BOMInputStream(super.getInputStream(), false, 
            ByteOrderMark.UTF_8, 
            ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE,
            ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
        try {
          if (bs.hasBOM()) {
            System.err.println("removing BOM " + bs.getBOM());
          }
          return bs;
        } catch (IOException e) {
          return super.getInputStream();
        }
      }
    };
  }

  public static AnalysisScope makeScope(String[] files, SingleClassLoaderFactory loaders, Language language) {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders, Collections.singleton(language));
    return result;
  }

  public static AnalysisScope makeScope(Module[] files, SingleClassLoaderFactory loaders, Language language) {
    CAstAnalysisScope result = new CAstAnalysisScope(files, loaders, Collections.singleton(language));
    return result;
  }

  public static IAnalysisCacheView makeCache(IRFactory<IMethod> factory) {
    return new AnalysisCacheImpl(factory);
  }

  public static String getShortName(CGNode nd) {
    IMethod method = nd.getMethod();
    return getShortName(method);
  }

  public static String getShortName(IMethod method) {
    String origName = method.getName().toString();
    String result = origName;
    if (origName.equals("do") || origName.equals("ctor")) {
      result = method.getDeclaringClass().getName().toString();
      result = result.substring(result.lastIndexOf('/') + 1);
      if (origName.equals("ctor")) {
        if (result.equals("LFunction")) {
          String s = method.toString();
          if (s.indexOf('(') != -1) {
            String functionName = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
            functionName = functionName.substring(functionName.lastIndexOf('/') + 1);
            result += " " + functionName;
          }
        }
        result = "ctor of " + result;
      }
    } 
    return result;
  }

  public static void dumpCG(SSAContextInterpreter interp, PointerAnalysis<InstanceKey> PA, CallGraph CG) {
    if (AVOID_DUMP)
      return;
    for (CGNode N : CG) {
      System.err.print("callees of node " + getShortName(N) + " : [");
      boolean fst = true;
      for (CGNode n : Iterator2Iterable.make(CG.getSuccNodes(N))) {
        if (fst)
          fst = false;
        else
          System.err.print(", ");
        System.err.print(getShortName(n));
      }
      System.err.println("]");
      System.err.println("\nIR of node " + N.getGraphNodeId() + ", context " + N.getContext());
      IRView ir = interp.getIRView(N);
      if (ir != null) {
        System.err.println(ir);
      } else {
        System.err.println("no IR!");
      }
    }

    System.err.println("pointer analysis");
    for (PointerKey n : PA.getPointerKeys()) {
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
          fileNames[i] = CAstCallGraphUtil.makeSourceModule(new File(fileNameArgs[i]).toURI().toURL(), fileNameArgs[i]);
        } catch (MalformedURLException e) {
          Assertions.UNREACHABLE(e.toString());
        }
      } else {
        URL url = CAstCallGraphUtil.class.getClassLoader().getResource(fileNameArgs[i]);
        fileNames[i] = CAstCallGraphUtil.makeSourceModule(url, fileNameArgs[i]);
      }
    }

    return fileNames;
  }

}
