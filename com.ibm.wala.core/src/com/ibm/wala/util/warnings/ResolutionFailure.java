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
 * A failure to resolve some entity while processing a particular
 * node
 * 
 * @author sfink
 */
public class ResolutionFailure<T> extends MethodWarning {

  final T ref;
  final String message;

  /**
   * @throws NullPointerException  if node is null
   */
  public ResolutionFailure(CGNode node, T ref, String message) throws NullPointerException {
    super(Warning.SEVERE, node.getMethod().getReference());
    this.message = message;
    this.ref = ref;
  }

  private ResolutionFailure(CGNode node, T ref) {
    this(node, ref, null);
  }

  /*
   * @see com.ibm.wala.util.warnings.Warning#getMsg()
   */
  @Override
  public String getMsg() {
    if (message == null) {
      return getClass() + " " + getMethod() + " " + ref;
    } else {
      return getClass() + " " + getMethod() + ": " + message + " for " + ref;
    }
  }

  public static <T> ResolutionFailure<T> create(CGNode node, T ref, String msg) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    return new ResolutionFailure<T>(node, ref, msg);
  }

  public static <T> ResolutionFailure create(CGNode node, T ref) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    return new ResolutionFailure<T>(node, ref);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result + ((ref == null) ? 0 : ref.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    final ResolutionFailure other = (ResolutionFailure) obj;
    if (message == null) {
      if (other.message != null)
        return false;
    } else if (!message.equals(other.message))
      return false;
    if (ref == null) {
      if (other.ref != null)
        return false;
    } else if (!ref.equals(other.ref))
      return false;
    return true;
  }


  

}
