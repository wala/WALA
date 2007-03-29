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
package com.ibm.wala.cast.java.client.impl;

import com.ibm.wala.cast.java.ipa.callgraph.*;
import com.ibm.wala.client.CallGraphBuilderFactory;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.cfa.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 * A factory to create call graph builders using 0-CFA
 */
public class ZeroOneContainerCFABuilderFactory implements CallGraphBuilderFactory {

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.domo.j2ee.client.CallGraphBuilderFactory#make(com.ibm.domo.ipa.callgraph.AnalysisOptions,
   *      com.ibm.domo.ipa.cha.ClassHierarchy, java.lang.ClassLoader,
   *      com.ibm.domo.j2ee.J2EEAnalysisScope,
   *      com.ibm.domo.util.warnings.WarningSet, boolean)
   */
  public CallGraphBuilder make(AnalysisOptions options,
			       ClassHierarchy cha,
			       AnalysisScope scope,
			       WarningSet warnings,
			       boolean keepPointsTo)
  {
      Util.addDefaultSelectors(options, cha, warnings);
      Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
      return new AstJavaZeroOneContainerCFABuilder(cha, warnings, options, null, null, options.getReflectionSpec());
  }
}
