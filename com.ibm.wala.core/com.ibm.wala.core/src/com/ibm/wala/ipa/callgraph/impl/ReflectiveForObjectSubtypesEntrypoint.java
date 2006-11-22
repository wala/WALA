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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class ReflectiveForObjectSubtypesEntrypoint extends ReflectiveSubtypesEntrypoint {

  public ReflectiveForObjectSubtypesEntrypoint(MethodReference method, ClassHierarchy cha) {
    super(method, cha);
  }

  public ReflectiveForObjectSubtypesEntrypoint(IMethod method, ClassHierarchy cha) {
    super(method, cha);
  }

  protected boolean useReflectiveMachinery(TypeReference type) {
    return type.equals(TypeReference.JavaLangObject);
  }

}
