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
package com.ibm.wala.util.warnings;

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * 
 * A failure to resolve some entity while processing a particular
 * node
 * 
 * @author sfink
 */
public class ResolutionFailure extends MethodWarning {

  final Object ref;
  final String message;

  /**
   * @param node
   * @param ref
   */
  public ResolutionFailure(CGNode node, Object ref, String message) {
    super(Warning.SEVERE, node.getMethod().getReference());
    this.message = message;
    this.ref = ref;
  }

  public ResolutionFailure(CGNode node, Object ref) {
    this(node, ref, null);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.warnings.Warning#getMsg()
   */
  public String getMsg() {
    if (message == null) {
      return getClass() + " " + getMethod() + " " + ref;
    } else {
      return getClass() + " " + getMethod() + ": " + message + " for " + ref;
    }
  }

  public static ResolutionFailure create(CGNode node, Object ref) {
    return new ResolutionFailure(node, ref);
  }

  public static ResolutionFailure create(CGNode node, Object ref, String msg) {
    return new ResolutionFailure(node, ref, msg);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      ResolutionFailure other = (ResolutionFailure)obj;
      return (getMethod().equals(other.getMethod()) && getLevel()==other.getLevel() && ref.equals(other.ref));
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return getMethod().hashCode() * 8999 + ref.hashCode() * 8461 + getLevel();
  }
  
  

}
