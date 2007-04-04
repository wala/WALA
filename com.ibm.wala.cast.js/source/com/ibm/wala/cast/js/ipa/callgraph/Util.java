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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import com.ibm.wala.cast.ipa.callgraph.*;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.loader.*;
import com.ibm.wala.cast.js.translator.*;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.cast.types.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.warnings.WarningSet;

public class Util extends com.ibm.wala.cast.ipa.callgraph.Util {

  private static JavaScriptTranslatorFactory translatorFactory =
    new JavaScriptTranslatorFactory.CAstRhinoFactory();
  
  public static void setTranslatorFactory(JavaScriptTranslatorFactory translatorFactory) {
    Util.translatorFactory = translatorFactory;
  }

  public static JavaScriptTranslatorFactory getTranslatorFactory() {
    return translatorFactory;
  }

  public static AnalysisOptions 
    makeOptions(AnalysisScope scope,
		boolean keepIRs,
		ClassHierarchy cha,
		Entrypoints roots,
		final WarningSet warnings)
  {
    final AnalysisOptions options = 
      new AnalysisOptions(scope, AstIRFactory.makeDefaultFactory(keepIRs), roots);

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    options.setSelector(new StandardFunctionTargetSelector(cha, options.getMethodTargetSelector()));

    options.setConstantType(Boolean.class, JavaScriptTypes.Boolean);
    options.setConstantType(String.class, JavaScriptTypes.String);
    options.setConstantType(Integer.class, JavaScriptTypes.Number);
    options.setConstantType(Float.class, JavaScriptTypes.Number);
    options.setConstantType(Double.class, JavaScriptTypes.Number);
    options.setConstantType(null, JavaScriptTypes.Null);
    
    options.setUseConstantSpecificKeys( true );

    options.setUseStacksForLexicalScoping( true );

    options.getSSAOptions().setPreserveNames( true );    

    return options;
  }
    
  public static JavaScriptLoaderFactory makeLoaders() {
    return new JavaScriptLoaderFactory(translatorFactory);
  }

  public static AnalysisScope 
    makeScope(String[] files, JavaScriptLoaderFactory loaders)
      throws IOException 
  {
    return new CAstAnalysisScope( files, loaders );
  }

  public static AnalysisScope 
    makeScope(SourceFileModule[] files, JavaScriptLoaderFactory loaders)
      throws IOException 
  {
    return new CAstAnalysisScope( files, loaders );
  }

  public static AnalysisScope 
    makeScope(URL[] files, JavaScriptLoaderFactory loaders)
      throws IOException 
  {
    return new CAstAnalysisScope( files, loaders );
  }

  public static ClassHierarchy 
    makeHierarchy(AnalysisScope scope,
		  ClassLoaderFactory loaders,
		  WarningSet warnings) 
      throws ClassHierarchyException
  {
    return ClassHierarchy.make(
      scope,
      loaders,
      warnings,
      JavaScriptTypes.Root);
  }

  public static Entrypoints makeScriptRoots(ClassHierarchy cha) { 
    return new JavaScriptEntryPoints(
      cha,
      cha.getLoader( JavaScriptTypes.jsLoader ));
  }

  public static Collection getNodes(CallGraph CG, String funName) {
    boolean ctor = funName.startsWith("ctor:");
    TypeReference TR = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName( "L"+(ctor?funName.substring(5):funName) ));
    MethodReference MR = ctor? JavaScriptMethods.makeCtorReference(TR): AstMethodReference.fnReference(TR);
    return CG.getNodes(MR);
  }
}
