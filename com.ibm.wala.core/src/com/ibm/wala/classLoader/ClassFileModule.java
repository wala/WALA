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

import java.io.File;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.shrike.ShrikeClassReaderHandle;
import com.ibm.wala.util.strings.ImmutableByteArray;

/**
 * A module which is a wrapper around a .class file
 */
public class ClassFileModule extends FileModule {

  private final String className;

  public ClassFileModule(File f) {
    super(f);
    // this is delicate: TODO, clean it up a bit.
    ShrikeClassReaderHandle reader = new ShrikeClassReaderHandle(this);
    ImmutableByteArray name = null;
    try {
      name = ImmutableByteArray.make(reader.get().getName());
    } catch (InvalidClassFileException e) {
      System.err.println("Invalid class file: " + f.getAbsolutePath());
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    className = name.toString();
  }


  @Override
  public String toString() {
    return "ClassFileModule:" + getFile();
  }

  public boolean isClassFile() {
    return true;
  }

  public String getClassName() {
    return className;
  }

  public boolean isSourceFile() {
    return false;
  }
}
