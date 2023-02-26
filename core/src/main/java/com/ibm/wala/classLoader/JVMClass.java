/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.classLoader;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Note that classes from JVML have some features that are not present in all "bytecode" languages
 * currently supported.
 *
 * @param <T> type of classloader which loads this format of class.
 */
public abstract class JVMClass<T extends IClassLoader> extends BytecodeClass<T> {

  protected JVMClass(T loader, IClassHierarchy cha) {
    super(loader, cha);
  }

  /** JVM-level modifiers; cached here for efficiency */
  protected int modifiers;

  @Override
  public int getModifiers() {
    return modifiers;
  }

  @Override
  public boolean isPublic() {
    boolean result = ((modifiers & Constants.ACC_PUBLIC) != 0);
    return result;
  }

  @Override
  public boolean isPrivate() {
    boolean result = ((modifiers & Constants.ACC_PRIVATE) != 0);
    return result;
  }

  @Override
  public boolean isInterface() {
    boolean result = ((modifiers & Constants.ACC_INTERFACE) != 0);
    return result;
  }

  /** @see com.ibm.wala.classLoader.IClass#isAbstract() */
  @Override
  public boolean isAbstract() {
    boolean result = ((modifiers & Constants.ACC_ABSTRACT) != 0);
    return result;
  }

  /** @see com.ibm.wala.classLoader.IClass#isSynthetic() */
  @Override
  public boolean isSynthetic() {
    boolean result = ((modifiers & Constants.ACC_SYNTHETIC) != 0);
    return result;
  }

  /** @see com.ibm.wala.classLoader.IClass#getClassInitializer() */
  @Override
  public IMethod getClassInitializer() {
    try {
      computeMethodMapIfNeeded();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    return methodMap.get(MethodReference.clinitSelector);
  }
}
