/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public class ArgumentInstanceContext implements Context {
  private final Context base;
  private final int index;
  private final InstanceKey instanceKey;

  // to easily identify when an ArgumentInstanceContext is present
  public static final ContextKey ID_KEY = new ContextKey() {};

  public ArgumentInstanceContext(Context base, int index, InstanceKey instanceKey) {
    this.base = base;
    this.index = index;
    this.instanceKey = instanceKey;
  }

  @Override
  public ContextItem get(ContextKey name) {
    /*if(name == ContextKey.RECEIVER && index == 1)
      return instanceKey;*/
    if(name.equals(ContextKey.PARAMETERS[index]) || name.equals(ID_KEY))
      return new FilteredPointerKey.SingleInstanceFilter(instanceKey);
    return base.get(name);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((base == null) ? 0 : base.hashCode());
    result = prime * result + index;
    result = prime * result + ((instanceKey == null) ? 0 : instanceKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ArgumentInstanceContext other = (ArgumentInstanceContext) obj;
    if (base == null) {
      if (other.base != null)
        return false;
    } else if (!base.equals(other.base))
      return false;
    if (index != other.index)
      return false;
    if (instanceKey == null) {
      if (other.instanceKey != null)
        return false;
    } else if (!instanceKey.equals(other.instanceKey))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ArgumentInstanceContext [base=" + base + ", index=" + index + ", instanceKey=" + instanceKey + "]";
  }
  
  
}
