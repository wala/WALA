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

import com.ibm.wala.cast.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author sfink
 * 
 * Common utilities for CFA-style call graph builders.
 */
public class JSCFABuilder extends JSSSAPropagationCallGraphBuilder {

  /**
   * @param cha
   * @param warnings
   */
  public JSCFABuilder(IClassHierarchy cha, WarningSet warnings, AnalysisOptions options) {
    super(cha, warnings, options, new AstCFAPointerKeys());
  }

}
