/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee.client.impl;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.j2ee.DeploymentMetaData;
import com.ibm.wala.j2ee.J2EEAnalysisScope;
import com.ibm.wala.j2ee.client.CallGraphBuilderFactory;
import com.ibm.wala.j2ee.util.Util;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * 
 * @author sfink
 */
public class ZeroOneContainerCFABuilderFactory
    extends com.ibm.wala.client.impl.ZeroOneContainerCFABuilderFactory
    implements CallGraphBuilderFactory 
{


  public CallGraphBuilder make(AnalysisOptions options, IClassHierarchy cha, J2EEAnalysisScope scope, DeploymentMetaData dmd,
      WarningSet warnings, boolean keepPointsTo) {
    return Util.makeZeroOneContainerCFABuilder(options, cha, getClass().getClassLoader(), scope, dmd, warnings);
  }

}
