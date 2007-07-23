/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;


public class BooleanConstant extends AbstractConstant {
  
  public static final BooleanConstant TRUE = new BooleanConstant(true);
  public static final BooleanConstant FALSE = new BooleanConstant(false);
  
  private boolean val;
  
  private BooleanConstant(boolean val) {
    this.val = val;
  }

  @Override
  public String toString() {
    return Boolean.toString(val);
  }

  @Override
  public boolean equals(Object obj) {
    // note that these are canonical
    return this == obj;
  }

  @Override
  public int hashCode() {
    return val ? 11 : 13;
  }

}
