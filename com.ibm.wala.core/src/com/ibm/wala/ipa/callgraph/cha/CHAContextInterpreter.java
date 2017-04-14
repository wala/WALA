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
package com.ibm.wala.ipa.callgraph.cha;

import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

public interface CHAContextInterpreter {

  /**
   * Does this object understand the given method? The caller had better check this before inquiring on other properties.
   */
  public boolean understands(CGNode node);
  
  /**
   * @return an Iterator of the call statements that may execute in a given method for a given context
   */
  public abstract Iterator<CallSiteReference> iterateCallSites(CGNode node);

  Iterator<NewSiteReference> iterateNewSites(CGNode node);
}
