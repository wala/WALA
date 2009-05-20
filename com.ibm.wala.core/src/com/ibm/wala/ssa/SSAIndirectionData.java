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
package com.ibm.wala.ssa;

import java.util.Collection;

public interface SSAIndirectionData<T extends SSAIndirectionData.Name> {

  public interface Name {  
  
  }
  
  Collection<T> getNames();
  
  int getDef(int instructionIndex, T name);
  
  void setDef(int instructionIndex, T name, int newDef);
  
  int getUse(int instructionIndex, T name);
  
  void setUse(int instructionIndex, T name, int newUse);
  
}
