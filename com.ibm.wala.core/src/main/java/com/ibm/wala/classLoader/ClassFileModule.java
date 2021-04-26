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

package com.ibm.wala.classLoader;

import com.ibm.wala.core.util.shrike.ShrikeClassReaderHandle;
import com.ibm.wala.core.util.strings.ImmutableByteArray;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import java.io.File;

/** A module which is a wrapper around a .class file */
public class ClassFileModule extends FileModule {

  private final String className;

  public ClassFileModule(File f, Module container) throws InvalidClassFileException {
    super(f, container);
    ShrikeClassReaderHandle reader = new ShrikeClassReaderHandle(this);
    ImmutableByteArray name = ImmutableByteArray.make(reader.get().getName());
    className = name.toString();
  }

  @Override
  public String toString() {
    return "ClassFileModule:" + getFile();
  }

  @Override
  public boolean isClassFile() {
    return true;
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public boolean isSourceFile() {
    return false;
  }
}
