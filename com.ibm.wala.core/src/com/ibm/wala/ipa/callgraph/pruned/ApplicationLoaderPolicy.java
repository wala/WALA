/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.pruned;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * Keeps a given CGNode if it stems from application code
 * @author Martin Mohr
 *
 */
public class ApplicationLoaderPolicy implements PruningPolicy {
  
  public static final ApplicationLoaderPolicy INSTANCE = new ApplicationLoaderPolicy();
  
  private ApplicationLoaderPolicy() {
    
  }
  
  @Override
  public boolean check(CGNode n) {
    return n.getMethod().getDeclaringClass().getClassLoader().getName().equals(AnalysisScope.APPLICATION);
  }
  
}
