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
package com.ibm.wala.cast.js.client;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.cast.ipa.callgraph.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.js.client.impl.*;
import com.ibm.wala.cast.js.ipa.callgraph.*;
import com.ibm.wala.cast.js.loader.*;
import com.ibm.wala.cast.js.translator.*;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.client.*;
import com.ibm.wala.client.impl.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.*;

import java.io.*;
import java.util.jar.*;

public class JavaScriptAnalysisEngine extends AbstractAnalysisEngine {

  protected JavaScriptLoaderFactory loaderFactory;

  protected JavaScriptTranslatorFactory translatorFactory;

  protected boolean keepIRs = true;

  public JavaScriptAnalysisEngine() {
    setCallGraphBuilderFactory(
      new com.ibm.wala.cast.js.client.impl.ZeroCFABuilderFactory() );
  }

  protected void buildAnalysisScope() {
    try {
      loaderFactory = new JavaScriptLoaderFactory(translatorFactory);

      SourceFileModule[] files = (SourceFileModule[])
        moduleFiles.toArray(new SourceFileModule[ moduleFiles.size() ]);

      scope = new CAstAnalysisScope( files, loaderFactory );
    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }

  protected ClassHierarchy buildClassHierarchy() {
    try {
      return 
        ClassHierarchy.make(
	  getScope(), loaderFactory, getWarnings(), JavaScriptTypes.Root);
    } catch (ClassHierarchyException e) {
      Assertions.UNREACHABLE(e.toString());
      return null;
    }
  }

  public void setTranslatorFactory(JavaScriptTranslatorFactory factory) {
      this.translatorFactory = factory;
  }

  public void setJ2SELibraries(JarFile[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  public void setJ2SELibraries(Module[] libs) {
    Assertions.UNREACHABLE("Illegal to call setJ2SELibraries");
  }

  public AnalysisOptions getDefaultOptions(Entrypoints roots) {
    final AnalysisOptions options = 
      new AnalysisOptions(
        scope, 
	AstIRFactory.makeDefaultFactory(keepIRs), 
	roots);

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
}
