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
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator.repository;

public class ToLowerCase extends CharTranslator {
  public ToLowerCase(int target) {
    super(target);
  }

  public ToLowerCase() {
    super();
  }

  protected char[] translate(char c) {
    return new char[] { Character.toLowerCase(c) };
  }
}