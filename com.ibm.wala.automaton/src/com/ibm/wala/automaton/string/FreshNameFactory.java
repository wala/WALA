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
package com.ibm.wala.automaton.string;

import java.util.Set;

import com.ibm.wala.automaton.AUtil;

public class FreshNameFactory {
  private Set<String> names;
  
  public FreshNameFactory(Set<String> usedVarNames) {
    this.names = usedVarNames;
  }
  
  public String createName(String name) {
    return AUtil.createUniqueName(name, names);
  }
}
