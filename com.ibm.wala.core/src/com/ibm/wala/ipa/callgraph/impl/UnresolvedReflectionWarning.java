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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.warnings.MethodWarning;


public class UnresolvedReflectionWarning extends MethodWarning {

  public UnresolvedReflectionWarning(CGNode node) throws NullPointerException {
    super(SEVERE,node.getMethod().getReference());
    
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.util.Warning#getMsg()
   */
  @Override
  public String getMsg() {
    return "Failed to hijack reflective factory node: " + getMethod();
  }

}