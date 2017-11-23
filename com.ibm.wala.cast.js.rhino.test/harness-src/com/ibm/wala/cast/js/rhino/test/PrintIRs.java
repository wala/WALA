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
package com.ibm.wala.cast.js.rhino.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.html.WebPageLoaderFactory;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;

public class PrintIRs {

  /**
   * prints the IR of each function in the script
   * 
   * @throws IOException
   * @throws ClassHierarchyException
   */
  public static void printIRsForJS(String filename) throws IOException, ClassHierarchyException {
    // use Rhino to parse JavaScript
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    // build a class hierarchy, for access to code info
    IClassHierarchy cha = JSCallGraphUtil.makeHierarchyForScripts(filename);
    printIRsForCHA(cha, t -> t.startsWith("Lprologue.js"));
  }

  protected static void printIRsForCHA(IClassHierarchy cha, Predicate<String> exclude) {
    // for constructing IRs
    IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();
    for (IClass klass : cha) {
      // ignore models of built-in JavaScript methods
      String name = klass.getName().toString();
      if (exclude.test(name)) continue;
      // get the IMethod representing the code
      IMethod m = klass.getMethod(AstMethodReference.fnSelector);
      if (m != null) {
        IR ir = factory.makeIR(m, Everywhere.EVERYWHERE, new SSAOptions());
        System.out.println(ir);
        if (m instanceof AstMethod) {
          AstMethod astMethod = (AstMethod) m;
          System.out.println(astMethod.getSourcePosition());
        }
        System.out.println("===================================================\n");
      }
    }
  }

  private static void printIRsForHTML(String filename) throws IllegalArgumentException, MalformedURLException, IOException,
      WalaException, Error {
    // use Rhino to parse JavaScript
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    // add model for DOM APIs
    JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
    URL url = (new File(filename)).toURI().toURL();
    Pair<Set<MappedSourceModule>, File> p = WebUtil.extractScriptFromHTML(url, DefaultSourceExtractor.factory);
    SourceModule[] scripts = p.fst.toArray(new SourceModule[] {});
    JavaScriptLoaderFactory loaders = new WebPageLoaderFactory(JSCallGraphUtil.getTranslatorFactory());
    CAstAnalysisScope scope = new CAstAnalysisScope(scripts, loaders, Collections.singleton(JavaScriptLoader.JS));
    IClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, JavaScriptLoader.JS);
    com.ibm.wala.cast.util.Util.checkForFrontEndErrors(cha);
    printIRsForCHA(cha, t -> t.startsWith("Lprologue.js") || t.startsWith("Lpreamble.js"));
  }

  /**
   * 
   * @param args
   * @throws IOException
   * @throws WalaException
   * @throws CancelException
   * @throws IllegalArgumentException
   * @throws Error
   */
  public static void main(String[] args) throws IOException, IllegalArgumentException, CancelException, WalaException, Error {
    String filename = args[0];
    if (filename.endsWith(".js")) {
      printIRsForJS(filename);
    } else if (filename.endsWith(".html")) {
      printIRsForHTML(filename);
    }

  }

}
