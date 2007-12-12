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
/*
 * Created on Sep 27, 2005
 */
package com.ibm.wala.cast.java.ipa.callgraph;

import java.util.*;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;

public class JavaSourceAnalysisScope extends AnalysisScope {

  public JavaSourceAnalysisScope() {
    this(Collections.singleton(Language.JAVA));
  }

  public JavaSourceAnalysisScope(Set languages) {
    super(languages);
    EclipseProjectPath.SOURCE_REF.setParent(getLoader(APPLICATION));
    getLoader(SYNTHETIC).setParent(EclipseProjectPath.SOURCE_REF);

    loadersByName.put(EclipseProjectPath.SOURCE, EclipseProjectPath.SOURCE_REF);

    setLoaderImpl(getLoader(SYNTHETIC), "com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader");
    setLoaderImpl(EclipseProjectPath.SOURCE_REF, "com.ibm.wala.cast.java.translator.polyglot.PolyglotSourceLoaderImpl");
  }

  public ClassLoaderReference getSourceLoader() {
    return getLoader(EclipseProjectPath.SOURCE);
  }
}
