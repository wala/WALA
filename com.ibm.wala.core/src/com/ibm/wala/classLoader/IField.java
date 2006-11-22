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

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

/**
 * @author sfink
 *
 */
public interface IField extends IMember {

  /**
   * @return the type of the  declared type of the field
   */
  public TypeReference getFieldTypeReference();
  
  /**
   * @return FieldReference representing this field
   */
  public FieldReference getFieldReference();
  
  /**
   * Is this field final?
   */
  public boolean isFinal();
  
  public boolean isPrivate();
  public boolean isProtected();
  public boolean isPublic();
  public boolean isStatic();

}
