/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.types;

import com.ibm.wala.types.TypeName;

public class AstTypeReference {

  public static final String rootTypeSourceStr = "Root";
  public static final String rootTypeDescStr = 'L' + rootTypeSourceStr;
  public static final TypeName rootTypeName = TypeName.string2TypeName(rootTypeDescStr);

  public static final String functionTypeSourceStr = "CodeBody";
  public static final String functionTypeDescStr = 'L' + functionTypeSourceStr;
  public static final TypeName functionTypeName = TypeName.string2TypeName(functionTypeDescStr);
}
