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
package com.ibm.wala.j2ee;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;

class EJBLifecycleEntrypoint extends DefaultEntrypoint {

  private final TypeReference bean;

  EJBLifecycleEntrypoint(IMethod m, ClassHierarchy cha, TypeReference bean) {
    super(m, cha);
    this.bean = bean;
  }
  
  public TypeReference[] getParameterTypes(int i) {
    if (i == 0) {
      // special logic for "this"
      return new TypeReference[] {bean};
    } else {
      return super.getParameterTypes(i);
    }
  }

}
