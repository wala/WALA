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
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 *
 * some utility support for dealing with "malleable" reflection allocations
 * 
 * @author sfink
 */
public class Malleable {
  private final static TypeName MalleableName = TypeName.string2TypeName("Lcom/ibm/wala/Malleable");
  public final static TypeReference Malleable = TypeReference.findOrCreate(ClassLoaderReference.Primordial, MalleableName);
  public final static TypeReference ExtMalleable = TypeReference.findOrCreate(ClassLoaderReference.Extension, MalleableName);

  public final static TypeReference MalleableCollection = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      "Lcom/ibm/wala/model/java/util/MalleableCollection");


  public static boolean isMalleable(TypeReference T) {
    if (T == null) {
      throw new IllegalArgumentException("T is null");
    }
    return T.equals(Malleable) || T.equals(ExtMalleable);
  }
}
