/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.shrikeCT;

import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyStore;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * This is a dumping ground for useful functions that manipulate class info.
 * 
 * @author roca@us.ibm.com
 */
public class CTUtils {
  public static void addClassToHierarchy(ClassHierarchyStore store, ClassReader cr) throws InvalidClassFileException,
      IllegalArgumentException {
    if (cr == null) {
      throw new IllegalArgumentException();
    }
    String[] superInterfaces = new String[cr.getInterfaceCount()];
    for (int i = 0; i < superInterfaces.length; i++) {
      superInterfaces[i] = CTDecoder.convertClassToType(cr.getInterfaceName(i));
    }
    String superName = cr.getSuperName();
    store.setClassInfo(CTDecoder.convertClassToType(cr.getName()), (cr.getAccessFlags() & Constants.ACC_INTERFACE) != 0, (cr
        .getAccessFlags() & Constants.ACC_FINAL) != 0, superName != null ? CTDecoder.convertClassToType(superName) : null,
        superInterfaces);
  }
}