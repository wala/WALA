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
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

class MDBEntrypoint extends EJBLifecycleEntrypoint {

  MDBEntrypoint(IMethod m, ClassHierarchy cha, TypeReference bean) {
    super(m, cha, bean);
    if (Assertions.verifyAssertions) {
      Assertions._assert(m.getDescriptor().equals(EJBConstants.onMessageDesc));
    }
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.Entrypoint#getParameterTypes(int)
   */
  public TypeReference[] getParameterTypes(int i) {
    if (i == 1) {
      return (TypeReference[]) EJBConstants.KnownMessages.clone();
    } else {
      return super.getParameterTypes(i);
    }
  }
}
