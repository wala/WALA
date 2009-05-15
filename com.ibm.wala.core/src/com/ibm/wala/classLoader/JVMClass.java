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
package com.ibm.wala.classLoader;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;

public abstract class JVMClass<T extends IClassLoader> extends BytecodeClass<T> {

  protected JVMClass(T loader, IClassHierarchy cha) {
    super(loader, cha);
  }

  /**
   * JVM-level modifiers; cached here for efficiency
   */
  protected int modifiers;

  public int getModifiers() {
    return modifiers;
  }

  public boolean isPublic() {
    boolean result = ((modifiers & Constants.ACC_PUBLIC) != 0);
    return result;
  }
  
  public boolean isPrivate() {
    boolean result = ((modifiers & Constants.ACC_PRIVATE) != 0);
    return result;
  }

  public boolean isInterface() {
    boolean result = ((modifiers & Constants.ACC_INTERFACE) != 0);
    return result;

  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isAbstract()
   */
  public boolean isAbstract() {
    boolean result = ((modifiers & Constants.ACC_ABSTRACT) != 0);
    return result;
  }
  
  /**
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  public IMethod getClassInitializer() {
    if (methodMap == null) {
      try {
        computeMethodMap();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }
    return methodMap.get(MethodReference.clinitSelector);
  }



}
