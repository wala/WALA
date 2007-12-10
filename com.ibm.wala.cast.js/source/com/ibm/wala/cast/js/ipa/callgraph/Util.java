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
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Collection;

import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

public class Util extends com.ibm.wala.cast.ipa.callgraph.Util {

  private static JavaScriptTranslatorFactory translatorFactory = new JavaScriptTranslatorFactory.CAstRhinoFactory();

  public static void setTranslatorFactory(JavaScriptTranslatorFactory translatorFactory) {
    Util.translatorFactory = translatorFactory;
  }

  public static JavaScriptTranslatorFactory getTranslatorFactory() {
    return translatorFactory;
  }
  
  public static AnalysisOptions makeOptions(AnalysisScope scope, boolean keepIRs, IClassHierarchy cha, Iterable<Entrypoint> roots) {
    final AnalysisOptions options = new AnalysisOptions(scope, /* AstIRFactory.makeDefaultFactory(keepIRs), */ roots);

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha);
    options.setSelector(new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector()));

    options.setUseConstantSpecificKeys(true);

    options.setUseStacksForLexicalScoping(true);

    options.getSSAOptions().setPreserveNames(true);

    return options;
  }

  public static JavaScriptLoaderFactory makeLoaders() {
    return new JavaScriptLoaderFactory(translatorFactory);
  }

  public static IClassHierarchy makeHierarchy(AnalysisScope scope, ClassLoaderFactory loaders)
      throws ClassHierarchyException {
    return ClassHierarchy.make(scope, loaders, JavaScriptLoader.JS);
  }

  public static Iterable<Entrypoint> makeScriptRoots(IClassHierarchy cha) {
    return new JavaScriptEntryPoints(cha, cha.getLoader(JavaScriptTypes.jsLoader));
  }

  public static Collection getNodes(CallGraph CG, String funName) {
    boolean ctor = funName.startsWith("ctor:");
    TypeReference TR = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName("L"
        + (ctor ? funName.substring(5) : funName)));
    MethodReference MR = ctor ? JavaScriptMethods.makeCtorReference(TR) : AstMethodReference.fnReference(TR);
    return CG.getNodes(MR);
  }
}
