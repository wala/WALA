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
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CFAPointerKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * @author sfink
 * 
 * Common utilities for CFA-style call graph builders.
 */
public class AstJavaCFABuilder extends AstJavaSSAPropagationCallGraphBuilder {

  /**
   * @param cha
   * @param warnings
   */
  public AstJavaCFABuilder(IClassHierarchy cha, AnalysisOptions options) {
    super(cha, options, new CFAPointerKeys());
  }

}
