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
package com.ibm.wala.logic;

/**
 * A special constant used for pattern matching during substitution.
 * 
 * @author sjfink
 */
public class Wildcard extends AbstractConstant {

  public static final Wildcard STAR = new Wildcard();
  
  private Wildcard() {
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public int hashCode() {
    return 151;
  }

  @Override
  public String toString() {
    return "*";
  }

}
