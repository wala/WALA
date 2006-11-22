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

package com.ibm.wala.classLoader;

import com.ibm.wala.types.TypeReference;

/**
 *
 * Represents a textual allocation site
 * 
 * @author dolby
 */
public class NewSiteReference extends ProgramCounter {

  /**
   * The type allocated
   */
  private final TypeReference declaredType;

  /**
   * @param programCounter bytecode index of the allocation site
   * @param declaredType declared type that is allocated
   */
  public NewSiteReference(int programCounter, TypeReference declaredType) {
    super(programCounter);
    this.declaredType = declaredType;
  }

  public TypeReference getDeclaredType() {
    return declaredType;
  }

  public static NewSiteReference make(int programCounter, TypeReference declaredType) {
    return new NewSiteReference(programCounter, declaredType);
  }
  
  public String toString() {
    return "NEW " + declaredType + "@" + getProgramCounter();
  }
}
